package tech.mamxanh.recipe.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class AiIngredientMentionGuard {
    private static final Logger LOG = LoggerFactory.getLogger(AiIngredientMentionGuard.class);
    private static final String WORD_BOUNDARY = "(?<![\\p{L}\\p{N}])%s(?![\\p{L}\\p{N}])";
    private static final Pattern DICING_PREFIX = Pattern.compile(
            "(?<![\\p{L}\\p{N}])(?:cắt|thái)(?:\\s+[\\p{L}\\p{N}]+){0,5}\\s+hạt\\s+$");
    private static final Pattern RINSING_PREFIX = Pattern.compile(
            "(?<![\\p{L}\\p{N}])(?:rửa|tráng)(?:\\s+"
                    + "(?!(?:rồi|sau|tiếp|thêm|cho|đổ|rót|chế|nấu|kho|đun|pha)(?![\\p{L}\\p{N}]))"
                    + "[\\p{L}\\p{N}]+){0,8}\\s+$");

    private AiIngredientMentionGuard() { }

    static void validate(String content, Set<Long> selectedIds, Set<String> selectedNames,
            List<IngredientResponse> catalog) {
        Set<String> allowed = new HashSet<>();
        selectedNames.stream().map(AiIngredientMentionGuard::normalize).forEach(allowed::add);
        catalog.stream().filter(item -> selectedIds.contains(item.id())).map(IngredientResponse::name)
                .map(AiIngredientMentionGuard::normalize).forEach(allowed::add);
        List<String> names = Stream.concat(catalog.stream().map(IngredientResponse::name)
                        .map(AiIngredientMentionGuard::normalize), allowed.stream())
                .distinct().sorted(Comparator.comparingInt(String::length).reversed()).toList();
        String remaining = normalize(content);
        for (String name : names) {
            String expression = WORD_BOUNDARY.formatted(Pattern.quote(name));
            // Sauce made from the selected ingredients is not an instruction to add water.
            if (name.equals("nước") && !allowed.contains(name)) {
                expression += "(?!\\s+(?:sốt|kho)(?![\\p{L}\\p{N}]))";
            }
            var matcher = Pattern.compile(expression).matcher(remaining);
            if (allowed.contains(name)) {
                // Match compound names first so "Nước tương" cannot become unselected "Nước".
                remaining = matcher.replaceAll(" ");
            } else {
                while (matcher.find()) {
                    String preceding = remaining.substring(0, matcher.start());
                    // Dicing and rinsing describe preparation, not additional recipe ingredients.
                    if ((name.equals("lựu") && DICING_PREFIX.matcher(preceding).find())
                            || (name.equals("nước") && RINSING_PREFIX.matcher(preceding).find())) continue;
                    LOG.warn("Recipe AI output mentions an unselected catalog ingredient: {}", name);
                    throw new AppException(ErrorCode.AI_RESPONSE_INVALID);
                }
            }
        }
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFC).toLowerCase(Locale.ROOT).strip();
    }
}
