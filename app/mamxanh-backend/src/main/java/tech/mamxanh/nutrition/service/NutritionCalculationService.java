package tech.mamxanh.nutrition.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import tech.mamxanh.nutrition.dto.response.NutritionProfileResponse.Target;

@Service
public class NutritionCalculationService {
    private static final MathContext MC = new MathContext(12);
    private static final String ENERGY_SOURCE = "National Academies of Sciences, Engineering, and Medicine, Dietary Reference Intakes for Energy (2023), Tables 5-15 and 5-16";
    private static final String ENERGY_URL = "https://www.nationalacademies.org/read/26818/chapter/7";
    private static final String MACRO_SOURCE = "National Academies of Sciences, Engineering, and Medicine, Dietary Reference Intakes for Energy, Carbohydrate, Fiber, Fat, Fatty Acids, Cholesterol, Protein, and Amino Acids (2005)";
    private static final String MACRO_URL = "https://nap.nationalacademies.org/catalog/10490/";
    private static final String ODS = "NIH Office of Dietary Supplements, fact sheets cross-checked with National Academies DRI";

    public int ageOn(LocalDate dob, LocalDate today) {
        return Period.between(dob, today).getYears();
    }

    public BigDecimal bmi(BigDecimal weightKg, BigDecimal heightCm) {
        BigDecimal heightM = heightCm.movePointLeft(2);
        return weightKg.divide(heightM.multiply(heightM, MC), MC);
    }

    public String bmiCategory(BigDecimal bmi) {
        if (bmi.compareTo(new BigDecimal("18.5")) < 0) return "Thiếu cân";
        if (bmi.compareTo(new BigDecimal("25")) < 0) return "Bình thường";
        if (bmi.compareTo(new BigDecimal("30")) < 0) return "Thừa cân";
        return "Béo phì";
    }

    public BigDecimal energyKcal(int age, String sex, BigDecimal heightCm, BigDecimal weightKg, String activity) {
        boolean male = "MALE".equals(sex);
        int activityIndex = switch (activity) {
            case "SEDENTARY" -> 0;
            case "LIGHTLY_ACTIVE" -> 1;
            case "MODERATELY_ACTIVE" -> 2;
            case "VERY_ACTIVE" -> 3;
            default -> throw new IllegalArgumentException("Unsupported activity level");
        };
        double a = age;
        double h = heightCm.doubleValue();
        double w = weightKg.doubleValue();
        double value;
        if (age == 18) {
            double[][] coefficients = male
                    ? new double[][] {{-447.51, 3.68, 13.01, 13.15}, {19.12, 3.68, 8.62, 20.28}, {-388.19, 3.68, 12.66, 20.46}, {-671.75, 3.68, 15.38, 23.25}}
                    : new double[][] {{55.59, -22.25, 8.43, 17.07}, {-297.54, -22.25, 12.77, 14.73}, {-189.55, -22.25, 11.74, 18.34}, {-709.59, -22.25, 18.22, 14.25}};
            double[] c = coefficients[activityIndex];
            value = c[0] + c[1] * a + c[2] * h + c[3] * w + 20;
        } else {
            double[][] coefficients = male
                    ? new double[][] {{753.07, -10.83, 6.50, 14.10}, {581.47, -10.83, 8.30, 14.94}, {1004.82, -10.83, 6.52, 15.91}, {-517.88, -10.83, 15.61, 19.11}}
                    : new double[][] {{584.90, -7.01, 5.72, 11.71}, {575.77, -7.01, 6.60, 12.14}, {710.25, -7.01, 6.54, 12.34}, {511.83, -7.01, 9.07, 12.56}};
            double[] c = coefficients[activityIndex];
            value = c[0] + c[1] * a + c[2] * h + c[3] * w;
        }
        return BigDecimal.valueOf(value);
    }

    public List<Target> dailyTargets(int age, String sex, BigDecimal energyKcal) {
        boolean male = "MALE".equals(sex);
        BigDecimal proteinLow = age == 18 ? new BigDecimal("0.10") : new BigDecimal("0.10");
        BigDecimal proteinHigh = age == 18 ? new BigDecimal("0.30") : new BigDecimal("0.35");
        BigDecimal carbLow = new BigDecimal("0.45");
        BigDecimal carbHigh = new BigDecimal("0.65");
        BigDecimal fatLow = age == 18 ? new BigDecimal("0.25") : new BigDecimal("0.20");
        BigDecimal fatHigh = new BigDecimal("0.35");
        List<Target> result = new ArrayList<>();
        result.add(target("protein", "Chất đạm", energyKcal.multiply(proteinLow, MC).divide(BigDecimal.valueOf(4), MC), energyKcal.multiply(proteinHigh, MC).divide(BigDecimal.valueOf(4), MC), "g", "AMDR", MACRO_URL));
        result.add(target("carbohydrate", "Carbohydrate", energyKcal.multiply(carbLow, MC).divide(BigDecimal.valueOf(4), MC), energyKcal.multiply(carbHigh, MC).divide(BigDecimal.valueOf(4), MC), "g", "AMDR", MACRO_URL));
        result.add(target("fat", "Chất béo", energyKcal.multiply(fatLow, MC).divide(BigDecimal.valueOf(9), MC), energyKcal.multiply(fatHigh, MC).divide(BigDecimal.valueOf(9), MC), "g", "AMDR", MACRO_URL));
        BigDecimal fiber = BigDecimal.valueOf(age == 18 ? (male ? 38 : 26) : age >= 51 ? (male ? 30 : 21) : (male ? 38 : 25));
        result.add(target("fiber", "Chất xơ", fiber, fiber, "g", "AI", MACRO_URL));
        int calcium = age == 18 ? 1300 : age >= 71 || (age >= 51 && !male) ? 1200 : 1000;
        result.add(target("calcium", "Canxi", bd(calcium), bd(calcium), "mg", "RDA", "https://ods.od.nih.gov/factsheets/Calcium-HealthProfessional/"));
        int iron = age == 18 ? (male ? 11 : 15) : age <= 50 && !male ? 18 : 8;
        result.add(target("iron", "Sắt", bd(iron), bd(iron), "mg", "RDA", "https://ods.od.nih.gov/factsheets/Iron-HealthProfessional/"));
        result.add(target("vitaminB12", "Vitamin B12", new BigDecimal("2.4"), new BigDecimal("2.4"), "mcg", "RDA", "https://ods.od.nih.gov/factsheets/VitaminB12-HealthProfessional/"));
        int zinc = age == 18 ? (male ? 11 : 9) : male ? 11 : 8;
        result.add(target("zinc", "Kẽm", bd(zinc), bd(zinc), "mg", "RDA", "https://ods.od.nih.gov/factsheets/Zinc-HealthProfessional/"));
        return result;
    }

    public String energySource() { return ENERGY_SOURCE + " — " + ENERGY_URL; }
    public String nutrientSource() { return MACRO_SOURCE + " — " + ODS; }

    private static BigDecimal bd(int value) { return BigDecimal.valueOf(value); }
    private static Target target(String key, String label, BigDecimal min, BigDecimal max, String unit, String type, String source) {
        return new Target(key, label, min, max, unit, type, source);
    }
}
