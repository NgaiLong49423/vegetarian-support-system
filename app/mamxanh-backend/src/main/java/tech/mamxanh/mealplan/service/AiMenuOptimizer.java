package tech.mamxanh.mealplan.service;

import tech.mamxanh.nutrition.dto.response.NutritionProfileResponse.Target;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministically combines trusted, one-serving recipes against the member's daily targets. */
final class AiMenuOptimizer {
    private static final List<String> MEALS = List.of("BREAKFAST", "LUNCH", "DINNER");
    private static final double[] MEAL_SHARES = {0.25, 0.375, 0.375};
    private static final double ENERGY_BUCKET = 0.05;

    List<Selection> optimize(LocalDate weekStart, LocalDate firstSuggestionDate, LocalDate lastSuggestionDate,
            double energyTarget, List<Candidate> ranked,
            List<Target> targets) {
        List<Selection> result = new ArrayList<>();
        Set<Long> previousDinner = Set.of();
        int firstDay = Math.toIntExact(java.time.temporal.ChronoUnit.DAYS.between(weekStart, firstSuggestionDate));
        int lastDay = Math.toIntExact(java.time.temporal.ChronoUnit.DAYS.between(weekStart, lastSuggestionDate));
        for (int day = Math.max(firstDay, 0); day <= Math.min(lastDay, 6); day++) {
            Set<Long> priorDinner = previousDinner;
            List<List<Candidate>> meals = new ArrayList<>();
            for (int slot = 0; slot < 3; slot++) meals.add(new ArrayList<>());
            for (int slot = 0; slot < MEALS.size(); slot++) {
                int currentSlot = slot;
                Candidate initial = ranked.stream()
                        .filter(item -> (currentSlot != 0 || priorDinner.isEmpty()
                                || priorDinner.stream().noneMatch(id -> id == item.id()))
                                && (currentSlot == 0 || meals.get(currentSlot - 1).stream()
                                        .noneMatch(previous -> previous.id() == item.id())))
                        .min(java.util.Comparator.comparingDouble(item -> Math.abs(value(item.nutrition(), "ENERGY_KCAL")
                                - energyTarget * MEAL_SHARES[currentSlot]) / Math.max(energyTarget, 1)
                                + ranked.indexOf(item) * 0.0001))
                        .orElse(ranked.get(0));
                meals.get(slot).add(initial);
            }

            boolean improved;
            do {
                improved = false;
                Score current = score(meals, energyTarget, targets, ranked);
                Candidate bestCandidate = null;
                int bestSlot = -1;
                Score bestScore = current;
                for (int slot = 0; slot < meals.size(); slot++) {
                    Set<Long> inSlot = meals.get(slot).stream().map(Candidate::id).collect(java.util.stream.Collectors.toSet());
                    for (Candidate candidate : ranked) {
                        if (inSlot.contains(candidate.id()) || adjacentContains(meals, slot, candidate.id())) continue;
                        meals.get(slot).add(candidate);
                        Score candidateScore = score(meals, energyTarget, targets, ranked);
                        meals.get(slot).remove(meals.get(slot).size() - 1);
                        if (candidateScore.betterThan(bestScore)) {
                            bestScore = candidateScore;
                            bestCandidate = candidate;
                            bestSlot = slot;
                        }
                    }
                }
                if (bestCandidate != null) {
                    meals.get(bestSlot).add(bestCandidate);
                    improved = true;
                }
            } while (improved);

            for (int slot = 0; slot < meals.size(); slot++) {
                for (Candidate candidate : meals.get(slot)) {
                    result.add(new Selection(weekStart.plusDays(day), MEALS.get(slot), candidate.id()));
                }
            }
            previousDinner = meals.get(2).stream().map(Candidate::id).collect(java.util.stream.Collectors.toSet());
        }
        return result;
    }

    private static boolean adjacentContains(List<List<Candidate>> meals, int slot, long id) {
        return (slot > 0 && meals.get(slot - 1).stream().anyMatch(candidate -> candidate.id() == id))
                || (slot + 1 < meals.size() && meals.get(slot + 1).stream().anyMatch(candidate -> candidate.id() == id));
    }

    private static Score score(List<List<Candidate>> meals, double target, List<Target> targets,
            List<Candidate> ranked) {
        double[] mealKcal = new double[3];
        Map<String, Double> actual = new java.util.HashMap<>();
        for (int slot = 0; slot < meals.size(); slot++) {
            for (Candidate candidate : meals.get(slot)) {
                candidate.nutrition().forEach((key, value) -> {
                    if (value != null) actual.merge(key, value.doubleValue(), Double::sum);
                });
                mealKcal[slot] += value(candidate.nutrition(), "ENERGY_KCAL");
            }
        }
        double totalKcal = java.util.Arrays.stream(mealKcal).sum();
        double energyError = (Math.abs(totalKcal - target) / Math.max(target, 1))
                + java.util.stream.IntStream.range(0, mealKcal.length)
                        .mapToDouble(slot -> Math.abs(mealKcal[slot] - target * MEAL_SHARES[slot]) / Math.max(target, 1))
                        .sum();
        int energyBand = (int) Math.floor(energyError / ENERGY_BUCKET);

        int met = 0;
        double deviation = 0;
        for (Target nutrient : targets) {
            String key = nutritionKey(nutrient.key());
            if ("ENERGY_KCAL".equals(key)) continue;
            Double value = actual.get(key);
            if (value == null || nutrient.minimum() == null) continue;
            double min = nutrient.minimum().doubleValue();
            Double max = nutrient.maximum() == null ? null : nutrient.maximum().doubleValue();
            boolean satisfied = value >= min && (!"AMDR".equals(nutrient.referenceType()) || (max != null && value <= max));
            if (satisfied) met++;
            if (value < min) deviation += (min - value) / Math.max(min, 1);
            else if (max != null && value > max) deviation += (value - max) / Math.max(max, 1);
        }
        int rankPenalty = meals.stream().flatMap(List::stream)
                .mapToInt(candidate -> ranked.indexOf(candidate)).sum();
        int inRangeMeals = 0;
        for (int slot = 0; slot < mealKcal.length; slot++) {
            double share = mealKcal[slot] / Math.max(target, 1);
            if (share >= MEAL_SHARES[slot] - 0.05 && share <= MEAL_SHARES[slot] + 0.05) inRangeMeals++;
        }
        return new Score(energyBand, inRangeMeals, energyError, met, deviation, rankPenalty);
    }

    private static String nutritionKey(String key) {
        return switch (key == null ? "" : key) {
            case "protein" -> "PROTEIN_G";
            case "carbohydrate" -> "CARBOHYDRATE_G";
            case "fat" -> "TOTAL_FAT_G";
            case "fiber" -> "FIBER_G";
            case "calcium" -> "CALCIUM_MG";
            case "iron" -> "IRON_MG";
            case "vitaminB12" -> "VITAMIN_B12_MCG";
            case "zinc" -> "ZINC_MG";
            default -> key;
        };
    }

    private static double value(Map<String, BigDecimal> nutrition, String key) {
        BigDecimal amount = nutrition.get(key);
        return amount == null ? 0 : amount.doubleValue();
    }

    record Candidate(long id, Map<String, BigDecimal> nutrition) { }
    record Selection(LocalDate date, String mealType, long recipeId) { }
    private record Score(int energyBand, int inRangeMeals, double energyError, int nutrientsMet, double nutrientDeviation,
            int rankPenalty) {
        boolean betterThan(Score other) {
            if (energyBand != other.energyBand) return energyBand < other.energyBand;
            if (inRangeMeals != other.inRangeMeals) return inRangeMeals > other.inRangeMeals;
            if (nutrientsMet != other.nutrientsMet) return nutrientsMet > other.nutrientsMet;
            if (Math.abs(nutrientDeviation - other.nutrientDeviation) > 1e-9) return nutrientDeviation < other.nutrientDeviation;
            if (Math.abs(energyError - other.energyError) > 1e-9) return energyError < other.energyError;
            return rankPenalty < other.rankPenalty;
        }
    }
}
