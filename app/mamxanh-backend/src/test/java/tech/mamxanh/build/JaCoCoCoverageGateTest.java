package tech.mamxanh.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JaCoCoCoverageGateTest {
    private static final Map<String, String> TYPES = Map.of(
            "LINE", "line", "BRANCH", "branch", "METHOD", "method", "INSTRUCTION", "instruction");

    @Test
    void acceptsAllCountersStrictlyAboveEightyPercent() throws Exception {
        assertTrue(JaCoCoCoverageGate.evaluate(report("81", "19")).passed());
    }

    @Test
    void rejectsExactlyEightyPercentForEachCounter() {
        for (String type : TYPES.keySet()) {
            assertFalse(evaluateCounter(type, "80", "20").passed(), type);
            assertFalse(evaluateCounter(type, "79", "21").passed(), type);
        }
    }

    @Test
    void usesExactCountsEvenWhenDisplayedPercentRoundsToEightyPointZero() throws Exception {
        JaCoCoCoverageGate.Evaluation evaluation = JaCoCoCoverageGate.evaluate(report("8001", "1999"));
        assertTrue(evaluation.passed());
        assertTrue(evaluation.markdown().contains("80.0% | >80% | PASS"));
    }

    @Test
    void rejectsMissingMalformedAndEmptyBundleCounters() {
        assertThrows(IllegalArgumentException.class, () -> JaCoCoCoverageGate.evaluate("<report/>"));
        assertThrows(Exception.class, () -> JaCoCoCoverageGate.evaluate("<report>"));
        assertThrows(IllegalArgumentException.class, () -> JaCoCoCoverageGate.evaluate(report("not-a-number", "1")));
        assertThrows(IllegalArgumentException.class, () -> JaCoCoCoverageGate.evaluate(report("0", "0")));
    }

    @Test
    void ignoresJaCoCoExternalDtdWithoutFetchingIt() throws Exception {
        String xml = report("81", "19").replace("<report>",
                "<!DOCTYPE report PUBLIC \"-//JACOCO//DTD Report 1.1//EN\" \"https://www.jacoco.org/jacoco/dtd/jacoco-report.dtd\"><report>");
        assertTrue(JaCoCoCoverageGate.evaluate(xml).passed());
    }

    private JaCoCoCoverageGate.Evaluation evaluateCounter(String target, String covered, String missed) {
        try {
            return JaCoCoCoverageGate.evaluate(report(TYPES.get(target).equals("line") ? covered : "81",
                    TYPES.get(target).equals("line") ? missed : "19", target, covered, missed));
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private static String report(String covered, String missed) {
        StringBuilder xml = new StringBuilder("<report>");
        TYPES.keySet().forEach(type -> xml.append(counter(type, covered, missed)));
        return xml.append("</report>").toString();
    }

    private static String report(String lineCovered, String lineMissed, String target, String covered, String missed) {
        StringBuilder xml = new StringBuilder("<report>");
        TYPES.keySet().forEach(type -> xml.append(counter(type,
                type.equals(target) ? covered : lineCovered, type.equals(target) ? missed : lineMissed)));
        return xml.append("</report>").toString();
    }

    private static String counter(String type, String covered, String missed) {
        return "<counter type=\"" + type + "\" covered=\"" + covered + "\" missed=\"" + missed + "\"/>";
    }
}
