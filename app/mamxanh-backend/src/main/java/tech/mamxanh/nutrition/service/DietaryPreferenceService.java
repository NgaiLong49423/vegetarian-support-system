package tech.mamxanh.nutrition.service;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.nutrition.dto.request.IngredientPreferenceListRequest;
import tech.mamxanh.nutrition.dto.request.SaveDietaryPreferencesRequest;
import tech.mamxanh.nutrition.dto.response.DietaryPreferencesResponse;
import tech.mamxanh.nutrition.dto.response.DietaryPreferencesResponse.AiPersonalization;
import tech.mamxanh.nutrition.dto.response.DietaryPreferencesResponse.IngredientPreferences;
import tech.mamxanh.nutrition.dto.response.DietaryPreferencesResponse.PreferenceItem;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.nutrition.dto.response.IngredientSuggestionResponse;
import tech.mamxanh.nutrition.dto.response.OnboardingInvitationResponse;
import tech.mamxanh.nutrition.entity.DietaryPreferenceEntity;
import tech.mamxanh.nutrition.entity.PreferenceType;
import tech.mamxanh.nutrition.entity.UserIngredientPreferenceEntity;
import tech.mamxanh.nutrition.repository.DietaryPreferenceRepository;
import tech.mamxanh.nutrition.repository.UserIngredientPreferenceRepository;

/**
 * FR-31: Onboarding questionnaire (UC-31.1), Skip (UC-31.2), viewing and updating the dietary
 * profile (UC-31.3) and the gate that must run before any personalized AI request (BR-31).
 */
@Service
public class DietaryPreferenceService {

    static final int MAX_SUGGESTIONS = 10;

    private final DietaryPreferenceRepository profiles;
    private final UserIngredientPreferenceRepository preferences;
    private final IngredientCatalogLookupService ingredientCatalog;
    private final Clock clock;

    public DietaryPreferenceService(DietaryPreferenceRepository profiles, UserIngredientPreferenceRepository preferences,
            IngredientCatalogLookupService ingredientCatalog, Clock clock) {
        this.profiles = profiles;
        this.preferences = preferences;
        this.ingredientCatalog = ingredientCatalog;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DietaryPreferencesResponse getOwnPreferences() {
        DietaryPreferenceEntity profile = currentMember();
        return response(profile, preferences.findByUserIdOrderByIdAsc(profile.getUserId()));
    }

    @Transactional
    public DietaryPreferencesResponse saveOwnPreferences(SaveDietaryPreferencesRequest request) {
        DietaryPreferenceEntity profile = currentMember();
        Map<String, IngredientResponse> standardByKey = ingredientCatalog.findActiveIngredients("").stream()
                .collect(Collectors.toMap(ingredient -> key(ingredient.name()), Function.identity(), (first, second) -> first));
        Map<String, PreferenceItem> avoid = normalize(request.avoid(), standardByKey);
        Map<String, PreferenceItem> dislike = normalize(request.dislike(), standardByKey);
        avoid.keySet().stream().filter(dislike::containsKey).findFirst().ifPresent(conflict -> {
            throw new AppException(ErrorCode.INGREDIENT_PREFERENCE_CONFLICT,
                    "\"" + avoid.get(conflict).name() + "\" không thể vừa là nguyên liệu cần tránh vừa là món không thích.");
        });

        LocalDateTime now = LocalDateTime.now(clock);
        profile.update(request.vegetarianType(), avoid.isEmpty() && request.avoid().confirmsNone(),
                dislike.isEmpty() && request.dislike().confirmsNone(), blankToNull(request.cuisinePreference()),
                request.maxCookingTimeMinutes(), request.preferredDifficulty(), now);
        preferences.deleteAllOfUser(profile.getUserId());
        List<UserIngredientPreferenceEntity> rows = new ArrayList<>();
        avoid.values().forEach(item -> rows.add(UserIngredientPreferenceEntity.of(profile.getUserId(),
                PreferenceType.AVOID, item.ingredientId(), item.name(), now)));
        dislike.values().forEach(item -> rows.add(UserIngredientPreferenceEntity.of(profile.getUserId(),
                PreferenceType.DISLIKE, item.ingredientId(), item.name(), now)));
        return response(profile, preferences.saveAll(rows));
    }

    @Transactional
    public void skipOnboarding() {
        currentMember().skipOnboarding(LocalDateTime.now(clock));
    }

    /**
     * AC-31.10: called by the client after each successful sign-in. Only the first call of an account
     * whose invitation is unanswered returns {@code true}; the row lock keeps parallel sign-ins from
     * both receiving it.
     */
    @Transactional
    public OnboardingInvitationResponse claimOnboardingInvitation() {
        DietaryPreferenceEntity profile = currentMember(profiles::findByIdForUpdate);
        return new OnboardingInvitationResponse(profile.recordOnboardingInvitation(LocalDateTime.now(clock)));
    }

    @Transactional(readOnly = true)
    public List<IngredientSuggestionResponse> suggestIngredients(String query) {
        currentMember();
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return ingredientCatalog.findActiveIngredients(query).stream()
                .limit(MAX_SUGGESTIONS)
                .map(ingredient -> new IngredientSuggestionResponse(ingredient.id(), ingredient.name(),
                        ingredient.ingredientGroup()))
                .toList();
    }

    /**
     * BR-31 gate for personalized AI (dish suggestions, weekly menus): call it before any work is
     * done for the request. It throws {@code 409 DIETARY_PROFILE_INCOMPLETE} with a {@code missing}
     * list, so no Gemini call and no partial Meal Plan entry can follow.
     */
    @Transactional(readOnly = true)
    public void requirePersonalizedAiEligible(long userId) {
        DietaryPreferenceEntity profile = profiles.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        List<DietaryRequirement> missing = missing(profile, preferences.findByUserIdOrderByIdAsc(userId));
        if (!missing.isEmpty()) {
            throw new AppException(ErrorCode.DIETARY_PROFILE_INCOMPLETE)
                    .withProperty("missing", missing.stream().map(Enum::name).toList());
        }
    }

    private DietaryPreferenceEntity currentMember() {
        return currentMember(profiles::findById);
    }

    private DietaryPreferenceEntity currentMember(Function<Long, Optional<DietaryPreferenceEntity>> loader) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        long userId;
        try {
            userId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        DietaryPreferenceEntity profile = loader.apply(userId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        if (!profile.isActiveMember()) {
            throw new AppException(ErrorCode.MEMBER_ACCESS_REQUIRED);
        }
        return profile;
    }

    /**
     * Trims, collapses spaces and merges duplicates (case-insensitive) of one list, keeping the
     * first spelling. A name equal to an active standard ingredient is linked to it.
     */
    private static Map<String, PreferenceItem> normalize(IngredientPreferenceListRequest list,
            Map<String, IngredientResponse> standardByKey) {
        Map<String, PreferenceItem> items = new LinkedHashMap<>();
        for (String raw : list.itemsOrEmpty()) {
            String name = clean(raw);
            if (name.isEmpty()) {
                // Bean Validation does not treat non-breaking spaces as blank; the CHECK constraint does.
                throw new AppException(ErrorCode.VALIDATION_FAILED, "Tên nguyên liệu không được để trống.");
            }
            String key = name.toLowerCase(Locale.ROOT);
            IngredientResponse standard = standardByKey.get(key);
            items.putIfAbsent(key, standard == null
                    ? new PreferenceItem(null, name)
                    : new PreferenceItem(standard.id(), standard.name()));
        }
        return items;
    }

    private static DietaryPreferencesResponse response(DietaryPreferenceEntity profile,
            List<UserIngredientPreferenceEntity> rows) {
        List<DietaryRequirement> missing = missing(profile, rows);
        return new DietaryPreferencesResponse(profile.getVegetarianType(),
                new IngredientPreferences(profile.isAvoidNoneConfirmed(), items(rows, PreferenceType.AVOID)),
                new IngredientPreferences(profile.isDislikeNoneConfirmed(), items(rows, PreferenceType.DISLIKE)),
                profile.getCuisinePreference(), profile.getMaxCookingTimeMinutes(), profile.getPreferredDifficulty(),
                profile.getOnboardingStatus(), new AiPersonalization(missing.isEmpty(), missing));
    }

    /** Data Dictionary rule 30: type chosen, and each list has a row or its "none" confirmation. */
    private static List<DietaryRequirement> missing(DietaryPreferenceEntity profile,
            List<UserIngredientPreferenceEntity> rows) {
        List<DietaryRequirement> missing = new ArrayList<>();
        if (profile.getVegetarianType() == null) {
            missing.add(DietaryRequirement.VEGETARIAN_TYPE);
        }
        if (!profile.isAvoidNoneConfirmed() && items(rows, PreferenceType.AVOID).isEmpty()) {
            missing.add(DietaryRequirement.AVOID_INGREDIENTS);
        }
        if (!profile.isDislikeNoneConfirmed() && items(rows, PreferenceType.DISLIKE).isEmpty()) {
            missing.add(DietaryRequirement.DISLIKED_INGREDIENTS);
        }
        return missing;
    }

    private static List<PreferenceItem> items(List<UserIngredientPreferenceEntity> rows, PreferenceType type) {
        return rows.stream().filter(row -> row.getPreferenceType() == type)
                .map(row -> new PreferenceItem(row.getIngredientId(), row.getName()))
                .toList();
    }

    private static String key(String name) {
        return clean(name).toLowerCase(Locale.ROOT);
    }

    private static String clean(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFC).replaceAll("[\\s\\u00A0]+", " ").trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
