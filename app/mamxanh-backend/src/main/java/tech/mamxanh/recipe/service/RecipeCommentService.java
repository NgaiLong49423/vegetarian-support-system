package tech.mamxanh.recipe.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.auth.service.CurrentUserService.PublicProfile;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.notification.service.NotificationService;
import tech.mamxanh.recipe.dto.request.CommentCreateRequest;
import tech.mamxanh.recipe.dto.request.CommentUpdateRequest;
import tech.mamxanh.recipe.dto.response.CommentResponse;
import tech.mamxanh.recipe.dto.response.RecipeCommentPageResponse;
import tech.mamxanh.recipe.entity.RecipeCommentEntity;
import tech.mamxanh.recipe.entity.RecipePostEntity;
import tech.mamxanh.recipe.repository.RecipeAuthorReferenceRepository;
import tech.mamxanh.recipe.repository.RecipeCommentRepository;
import tech.mamxanh.recipe.repository.RecipePostRepository;

/**
 * FR-46 threaded comments on published recipes (BR-66). Guests read; an ACTIVE Customer or Expert
 * writes, and only the owner edits or deletes. Content is plain text that clients render as text
 * (AC-46.3). A deleted comment stays as a tombstone while it still has a visible reply (AC-46.2).
 * A reply to another member's comment notifies that member in the same transaction (FR-49, AC-49.1).
 */
@Service
public class RecipeCommentService {
    private static final String PUBLISHED = "PUBLISHED";
    private static final Comparator<RecipeCommentEntity> CONVERSATION_ORDER =
            Comparator.comparing(RecipeCommentEntity::getCreatedAt).thenComparing(RecipeCommentEntity::getId);

    private final RecipeCommentRepository commentRepository;
    private final RecipePostRepository recipeRepository;
    private final RecipeAuthorReferenceRepository accountRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final Clock clock;

    public RecipeCommentService(RecipeCommentRepository commentRepository, RecipePostRepository recipeRepository,
            RecipeAuthorReferenceRepository accountRepository, CurrentUserService currentUserService,
            NotificationService notificationService, Clock clock) {
        this.commentRepository = commentRepository;
        this.recipeRepository = recipeRepository;
        this.accountRepository = accountRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RecipeCommentPageResponse list(long recipeId, int page, int size) {
        requirePublishedRecipe(recipeId);
        List<Long> rootIds = commentRepository.findVisibleRootIds(recipeId, (long) page * size, size);
        List<RecipeCommentEntity> comments = rootIds.isEmpty() ? List.of() : commentRepository.findThreads(rootIds);
        Map<Long, List<RecipeCommentEntity>> repliesByParent = comments.stream()
                .filter(comment -> comment.getParentCommentId() != null)
                .sorted(CONVERSATION_ORDER)
                .collect(Collectors.groupingBy(RecipeCommentEntity::getParentCommentId));
        Map<Long, PublicProfile> authors = currentUserService.getPublicProfiles(
                comments.stream().map(RecipeCommentEntity::getUserId).distinct().toList());
        Map<Long, RecipeCommentEntity> roots = comments.stream()
                .filter(comment -> comment.getParentCommentId() == null)
                .collect(Collectors.toMap(RecipeCommentEntity::getId, comment -> comment));
        List<CommentResponse> threads = rootIds.stream()
                .map(id -> toVisibleResponse(roots.get(id), repliesByParent, authors))
                .toList();
        long totalThreads = commentRepository.countVisibleRoots(recipeId);
        return new RecipeCommentPageResponse(threads, page, size, totalThreads,
                (int) Math.ceil((double) totalThreads / size), commentRepository.countByRecipeIdAndDeletedFalse(recipeId));
    }

    @Transactional
    public CommentResponse create(long recipeId, CommentCreateRequest request) {
        long userId = requireActiveMember();
        RecipePostEntity recipe = requirePublishedRecipe(recipeId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (request.parentCommentId() == null) {
            RecipeCommentEntity comment = RecipeCommentEntity.root(recipeId, userId, request.content(), now);
            commentRepository.save(comment);
            return single(comment);
        }
        RecipeCommentEntity target = replyTarget(recipeId, request.parentCommentId());
        RecipeCommentEntity comment = RecipeCommentEntity.replyTo(replyParent(target), userId, request.content(), now);
        commentRepository.save(comment);
        CommentResponse created = single(comment);
        if (target.getUserId() != userId) {
            notificationService.commentReply(target.getUserId(), comment.getId(), recipeId, recipe.getTitle(),
                    created.author().displayName());
        }
        return created;
    }

    @Transactional
    public CommentResponse update(long commentId, CommentUpdateRequest request) {
        long userId = requireActiveMember();
        RecipeCommentEntity comment = requireOwnComment(commentId, userId);
        if (comment.isDeleted()) {
            throw new AppException(ErrorCode.COMMENT_NOT_FOUND);
        }
        comment.edit(request.content(), LocalDateTime.now(clock));
        return single(comment);
    }

    /** Idempotent for the owner; a comment that is already deleted stays deleted. */
    @Transactional
    public void delete(long commentId) {
        long userId = requireActiveMember();
        RecipeCommentEntity comment = requireOwnComment(commentId, userId);
        if (!comment.isDeleted()) {
            comment.markDeleted(LocalDateTime.now(clock));
        }
    }

    /** The comment the member answers; it must be live and on the same recipe (BR-66). */
    private RecipeCommentEntity replyTarget(long recipeId, long targetId) {
        return commentRepository.findById(targetId)
                .filter(comment -> comment.getRecipeId() == recipeId && !comment.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
    }

    /** UC-46 step 4: a reply to a level-5 comment joins its level-4 parent so the thread never gets deeper. */
    private RecipeCommentEntity replyParent(RecipeCommentEntity target) {
        if (target.getDepth() < RecipeCommentEntity.MAX_DEPTH) {
            return target;
        }
        return commentRepository.findById(target.getParentCommentId())
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private RecipeCommentEntity requireOwnComment(long commentId, long userId) {
        RecipeCommentEntity comment = commentRepository.findById(commentId)
                .filter(found -> recipeRepository.findByIdAndStatus(found.getRecipeId(), PUBLISHED).isPresent())
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        if (comment.getUserId() != userId) {
            throw new AppException(ErrorCode.COMMENT_EDIT_NOT_ALLOWED);
        }
        return comment;
    }

    private RecipePostEntity requirePublishedRecipe(long recipeId) {
        return recipeRepository.findByIdAndStatus(recipeId, PUBLISHED)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));
    }

    /** BR-05, FR-46 actors: only an ACTIVE Customer or Expert writes comments. */
    private long requireActiveMember() {
        long userId = currentUserService.requireAuthenticatedUserId();
        accountRepository.findById(userId)
                .filter(account -> "ACTIVE".equals(account.getAccountStatus())
                        && ("CUSTOMER".equals(account.getRole()) || "EXPERT".equals(account.getRole())))
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_ACCESS_REQUIRED,
                        "Chỉ Member có tài khoản hoạt động mới bình luận được."));
        return userId;
    }

    private CommentResponse single(RecipeCommentEntity comment) {
        return toResponse(comment, currentUserService.getPublicProfiles(List.of(comment.getUserId())), List.of());
    }

    /** A deleted comment is kept only while one of its replies is still visible; otherwise it is null. */
    private CommentResponse toVisibleResponse(RecipeCommentEntity comment,
            Map<Long, List<RecipeCommentEntity>> repliesByParent, Map<Long, PublicProfile> authors) {
        List<CommentResponse> replies = repliesByParent.getOrDefault(comment.getId(), List.of()).stream()
                .map(reply -> toVisibleResponse(reply, repliesByParent, authors))
                .filter(Objects::nonNull)
                .toList();
        if (comment.isDeleted() && replies.isEmpty()) {
            return null;
        }
        return toResponse(comment, authors, replies);
    }

    private static CommentResponse toResponse(RecipeCommentEntity comment, Map<Long, PublicProfile> authors,
            List<CommentResponse> replies) {
        PublicProfile profile = comment.isDeleted() ? null : authors.get(comment.getUserId());
        CommentResponse.Author author = profile == null ? null
                : new CommentResponse.Author(profile.id(), profile.displayName(), profile.avatarUrl());
        return new CommentResponse(comment.getId(), comment.getParentCommentId(), comment.getDepth(),
                comment.isDeleted() ? null : comment.getContent(), author, comment.isDeleted(), comment.isEdited(),
                comment.getCreatedAt(), replies);
    }
}
