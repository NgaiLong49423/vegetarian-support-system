package tech.mamxanh.recipe.entity;

import java.util.Arrays;

public final class RecipeCodes {
    private RecipeCodes() {}

    public enum DishCategory {
        NOODLE_SOUP("Món nước"), STIR_FRY("Món xào"), HOT_POT("Món lẩu"), BRAISED("Món kho"),
        SOUP("Món canh"), FRIED("Món chiên"), STEAMED("Món hấp"), SALAD("Món gỏi / salad"),
        ROLL("Món cuốn"), GRILLED("Món nướng"), DESSERT("Món tráng miệng / chè");

        private final String label;
        DishCategory(String label) { this.label = label; }
        public String label() { return label; }
    }

    public enum VegetarianType {
        VEGAN("Thuần chay"), LACTO("Có sữa"), OVO("Có trứng"), LACTO_OVO("Có trứng và sữa");
        private final String label;
        VegetarianType(String label) { this.label = label; }
        public String label() { return label; }
    }

    public enum Difficulty {
        EASY("Dễ"), MEDIUM("Trung bình"), HARD("Khó");
        private final String label;
        Difficulty(String label) { this.label = label; }
        public String label() { return label; }
    }

    public enum MediaType {
        JPEG("image/jpeg"), PNG("image/png"), WEBP("image/webp");
        private final String mimeType;
        MediaType(String mimeType) { this.mimeType = mimeType; }
        public String mimeType() { return mimeType; }

        public static boolean accepts(String mimeType) {
            return Arrays.stream(values()).anyMatch(type -> type.mimeType.equalsIgnoreCase(mimeType));
        }
    }
}
