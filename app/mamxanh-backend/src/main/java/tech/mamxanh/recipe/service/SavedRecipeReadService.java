package tech.mamxanh.recipe.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.recipe.dto.response.SavedRecipeResponse;
import tech.mamxanh.recipe.repository.SavedRecipeRepository;

@Service
public class SavedRecipeReadService {
    private static final int MAX_PAGE_SIZE = 50;
    private final CurrentUserService currentUserService;
    private final SavedRecipeRepository repository;

    public SavedRecipeReadService(CurrentUserService currentUserService, SavedRecipeRepository repository) {
        this.currentUserService = currentUserService;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<SavedRecipeResponse> list(String keyword, int page, int size) {
        String query = keyword == null ? "" : keyword.trim();
        if (query.length() > 120 || page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "keyword tối đa 120 ký tự, page phải >= 0 và size phải trong khoảng 1 đến 50.");
        }
        long userId = currentUserService.requireActiveMember().id();
        var results = repository.findSavedRecipeProjections(userId, query, PageRequest.of(page, size));
        return PageResponse.of(results.getContent().stream().map(item -> {
            boolean available = Boolean.TRUE.equals(item.getAvailable());
            return new SavedRecipeResponse(item.getRecipeId(), item.getTitle(), item.getDescription(),
                    item.getCoverUrl(), item.getAuthorName(),
                    available, available ? null : "Công thức không còn khả dụng", item.getSavedAt());
        }).toList(), page, size, results.getTotalElements());
    }
}
