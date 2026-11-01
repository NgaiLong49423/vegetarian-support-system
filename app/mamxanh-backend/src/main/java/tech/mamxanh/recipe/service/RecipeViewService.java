package tech.mamxanh.recipe.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.recipe.dto.response.RecipeViewResponse;
import tech.mamxanh.recipe.repository.RecipeViewRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RecipeViewService {
    private final RecipeViewRepository repository;
    private final Clock clock;

    public RecipeViewService(RecipeViewRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** userId comes exclusively from the verified JWT, never from the request body. */
    @Transactional
    public RecipeViewResponse record(long recipeId, Long userId, String anonymousViewerId) {
        if (recipeId <= 0) throw new AppException(ErrorCode.VALIDATION_FAILED);
        String hash = userId == null ? hashAnonymous(anonymousViewerId) : null;
        String viewer = userId == null ? "guest:" + hash : "user:" + userId;
        // SQL Server begins the JDBC implicit transaction on the first table statement.
        // sp_getapplock alone cannot start it. Read before locking, then read again
        // under the lock so a concurrent counted event cannot return a stale counter.
        repository.publishedCount(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));
        if (repository.lockViewer("recipe-view:" + recipeId + ":" + viewer) < 0) {
            throw new AppException(ErrorCode.RECIPE_VIEW_UNAVAILABLE);
        }
        long count = repository.publishedCount(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));
        // Take the timestamp after acquiring the lock; waiting must not create an old event.
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        var latest = repository.latest(recipeId, userId, hash);
        if (latest.isPresent() && !latest.get().isBefore(now.minusMinutes(30))) {
            return new RecipeViewResponse(recipeId, false, count);
        }
        long updatedCount = repository.incrementPublishedCount(recipeId)
                .orElseThrow(() -> new AppException(ErrorCode.RECIPE_NOT_FOUND));
        repository.insert(recipeId, userId, hash, now);
        return new RecipeViewResponse(recipeId, true, updatedCount);
    }

    private String hashAnonymous(String value) {
        if (value == null || !value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Thiếu định danh phiên khách hợp lệ.");
        }
        String canonical = UUID.fromString(value).toString();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
