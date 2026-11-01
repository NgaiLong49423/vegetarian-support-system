package tech.mamxanh.recipe.service;

import tech.mamxanh.recipe.dto.request.AiChatRequest;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Local routing only: never asks the provider to decide whether a paid operation is allowed. */
final class AiChatIntent {
    private AiChatIntent() { }

    enum Workflow { WEEKLY_MENU, RECIPE_AUTHORING }

    static Workflow workflow(AiChatRequest request) {
        String question = normalize(request.question());
        Workflow direct = workflow(question);
        if (direct != null) return direct;
        if (question.matches("(?:hay |cu )?(?:lam di|tao di|lap di|tiep tuc|dong y|yes|do it|go ahead)[.!?]*")) {
            return workflow(lastUserQuestion(request));
        }
        return null;
    }

    private static Workflow workflow(String question) {
        // Only a standalone definition question is exempt; appending an operation must not bypass routing.
        if (question.matches("(?:thuc don tuan|thuc don 7 ngay|menu tuan) (?:la gi|co nghia la gi)[?.!]*")
                || question.matches("what is (?:a |the )?(?:weekly |7-day )?meal[- ]plan[?.!]*")) return null;
        boolean menu = has(question, "thuc don", "meal plan", "meal-plan", "menu", "ke hoach an", "lich an");
        if (menu && has(question, "tuan", "7 ngay", "bay ngay", "7-day", "7 day", "seven day", "week",
                "theo dinh duong", "dinh duong ca nhan", "theo ho so", "personalized", "personalised")) return Workflow.WEEKLY_MENU;
        String authoringAction = question.replace("bai viet", "post").replace("bai dang", "post").replace("bai cong thuc", "post");
        if (has(question, "bai dang", "bai viet", "bai cong thuc", "recipe post")
                && Pattern.compile("\\b(?:viet|soan|tao|write|draft)\\b").matcher(authoringAction).find()) {
            return Workflow.RECIPE_AUTHORING;
        }
        return null;
    }

    static boolean moreRecipes(String question) {
        return Pattern.compile("\\b(?:mon (?:nao )?khac|mon an khac|con .*mon|them mon|more (?:dishes|recipes)|other (?:dishes|recipes)|anything else)\\b")
                .matcher(normalize(question)).find();
    }

    static boolean discovery(AiChatRequest request) {
        String question = normalize(request.question());
        if (directDiscovery(question)) return true;
        return moreRecipes(question) && (question.contains("mon") || turns(request).stream()
                .anyMatch(turn -> turn.speaker() == AiChatRequest.Speaker.USER && directDiscovery(normalize(turn.content())))
                || turns(request).stream().anyMatch(turn -> turn.speaker() == AiChatRequest.Speaker.ASSISTANT
                        && turn.recommendedRecipeIds() != null && !turn.recommendedRecipeIds().isEmpty()));
    }

    private static boolean directDiscovery(String question) {
        return has(question, "goi y", "de xuat", "tim mon", "tim cong thuc", "cong thuc", "mon nao", "mon gi",
                "mon chay nao", "mon chay gi", "mon de", "mon nhanh", "mon chay de", "mon chay nhanh",
                "de nau", "bai viet", "blog", "recipe", "recommend", "find dishes", "suggest dishes", "bien tau", "cai bien")
                || question.contains("mon ") && category(question) != null;
    }

    static String category(AiChatRequest request) {
        String current = category(request.question());
        if (current != null || !moreRecipes(request.question())) return current;
        List<AiChatRequest.Turn> history = turns(request);
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).speaker() == AiChatRequest.Speaker.USER) {
                String earlier = category(history.get(i).content());
                if (earlier != null) return earlier;
            }
        }
        return null;
    }

    private static String category(String question) {
        // Preserve accents here: khổ qua/khô/khó are not kho, and lâu is not lẩu.
        question = Normalizer.normalize(question, Normalizer.Form.NFC).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ")
                .replaceAll("(?U)\\b(?:gợi ý|goi y)\\b", "suggest")
                .replaceAll("(?U)\\bkho (?:công thức|cong thuc|dữ liệu|du lieu|nguyên liệu|nguyen lieu)\\b", "catalog");
        if (has(question, "gỏi cuốn", "goi cuon")) return "ROLL";
        String[][] categories = {{"NOODLE_SOUP", "món nước", "mon nuoc", "noodle soup"}, {"BRAISED", "kho", "braised"},
                {"STIR_FRY", "xào", "xao", "stir fry"}, {"HOT_POT", "lẩu", "lau", "hot pot"}, {"SOUP", "canh", "soup"},
                {"FRIED", "chiên", "chien", "fried"}, {"STEAMED", "hấp", "hap", "steamed"}, {"SALAD", "gỏi", "goi", "salad"},
                {"ROLL", "cuốn", "cuon", "roll"}, {"GRILLED", "nướng", "nuong", "grilled"},
                {"DESSERT", "tráng miệng", "trang mieng", "chè", "che", "dessert"}};
        for (String[] entry : categories) {
            for (int i = 1; i < entry.length; i++) {
                if (Pattern.compile("(?U)\\b" + Pattern.quote(entry[i]) + "\\b").matcher(question).find()) return entry[0];
            }
        }
        return null;
    }

    private static String lastUserQuestion(AiChatRequest request) {
        List<AiChatRequest.Turn> history = turns(request);
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).speaker() == AiChatRequest.Speaker.USER) return normalize(history.get(i).content());
        }
        return "";
    }

    static List<AiChatRequest.Turn> turns(AiChatRequest request) {
        return request.history() == null ? List.of() : request.history();
    }

    static String normalize(String value) {
        return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replace('đ', 'd').replaceAll("\\s+", " ").strip();
    }

    private static boolean has(String value, String... terms) {
        return java.util.Arrays.stream(terms).anyMatch(value::contains);
    }
}
