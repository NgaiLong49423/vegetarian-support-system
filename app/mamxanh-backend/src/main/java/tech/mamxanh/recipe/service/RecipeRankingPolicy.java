package tech.mamxanh.recipe.service;

/** Stable ranking weights defined by BR-71 and the agreed BR-72 completion. */
public final class RecipeRankingPolicy {
    public static final int VIEW_WEIGHT = 1;
    public static final int COMMENT_WEIGHT = 5;
    public static final int REACTION_WEIGHT = 10;
    public static final int TRENDING_NEW_RECIPE_HOURS = 48;
    public static final int TRENDING_NEW_RECIPE_BONUS = 1;
    public static final double TRENDING_FRESHNESS_EXPONENT = 1.5;

    private RecipeRankingPolicy() { }
}
