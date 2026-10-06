package tech.mamxanh.build;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/** Validates and summarizes JaCoCo's aggregate counters using the strict >80% project gate. */
public final class JaCoCoCoverageGate {
    private static final List<String> COUNTERS = List.of("LINE", "BRANCH", "METHOD", "INSTRUCTION");

    private JaCoCoCoverageGate() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: JaCoCoCoverageGate <jacoco.xml> <summary.md>");
        }

        Path report = Path.of(args[0]);
        Path summary = Path.of(args[1]);
        Evaluation evaluation;
        try {
            evaluation = evaluate(Files.readString(report));
        } catch (Exception error) {
            throw new IllegalStateException("Backend coverage gate could not read or validate JaCoCo report: "
                    + error.getMessage(), error);
        }
        Files.createDirectories(summary.getParent());
        Files.writeString(summary, evaluation.markdown());
        System.out.print(evaluation.markdown());
        if (!evaluation.passed()) {
            throw new IllegalStateException("Backend coverage gate failed: every JaCoCo BUNDLE counter must be strictly greater than 80%.");
        }
    }

    static Evaluation evaluate(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        var builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
        Document document = builder.parse(new InputSource(new StringReader(xml)));
        Element root = document.getDocumentElement();
        if (root == null || !"report".equals(root.getTagName())) {
            throw new IllegalArgumentException("Expected a JaCoCo <report> document.");
        }

        List<Metric> metrics = new ArrayList<>();
        for (String type : COUNTERS) {
            Element counter = findBundleCounter(root, type);
            if (counter == null) {
                throw new IllegalArgumentException("JaCoCo report is missing BUNDLE " + type + " counter.");
            }
            BigInteger covered = parseCount(counter, "covered", type);
            BigInteger missed = parseCount(counter, "missed", type);
            BigInteger total = covered.add(missed);
            if (covered.signum() < 0 || missed.signum() < 0 || total.signum() == 0) {
                throw new IllegalArgumentException("JaCoCo BUNDLE " + type + " counter has invalid or empty counts.");
            }
            metrics.add(new Metric(type, covered, total, covered.multiply(BigInteger.valueOf(5))
                    .compareTo(total.multiply(BigInteger.valueOf(4))) > 0));
        }
        return new Evaluation(List.copyOf(metrics));
    }

    private static Element findBundleCounter(Element root, String type) {
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element element && "counter".equals(element.getTagName())
                    && type.equals(element.getAttribute("type"))) {
                return element;
            }
        }
        return null;
    }

    private static BigInteger parseCount(Element counter, String attribute, String type) {
        String value = counter.getAttribute(attribute);
        if (value.isBlank()) {
            throw new IllegalArgumentException("JaCoCo BUNDLE " + type + " counter is missing " + attribute + " count.");
        }
        try {
            return new BigInteger(value);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("JaCoCo BUNDLE " + type + " counter has an invalid " + attribute + " count.", error);
        }
    }

    record Metric(String type, BigInteger covered, BigInteger total, boolean passed) {
        String markdown() {
            String display = new BigDecimal(covered).multiply(BigDecimal.valueOf(100))
                    .divide(new BigDecimal(total), 1, RoundingMode.HALF_UP).toPlainString() + "%";
            return "| " + label(type) + " | " + covered + "/" + total + " | " + display + " | >80% | "
                    + (passed ? "PASS" : "FAIL") + " |";
        }
    }

    record Evaluation(List<Metric> metrics) {
        boolean passed() {
            return metrics.stream().allMatch(Metric::passed);
        }

        String markdown() {
            return "## Backend — JaCoCo coverage\n\n"
                    + "| Metric | Covered/total | Coverage | Required | Gate |\n"
                    + "| --- | ---: | ---: | ---: | :---: |\n"
                    + metrics.stream().map(Metric::markdown).reduce((left, right) -> left + "\n" + right).orElseThrow()
                    + "\n";
        }
    }

    private static String label(String type) {
        return switch (type) {
            case "LINE" -> "Lines";
            case "BRANCH" -> "Branches";
            case "METHOD" -> "Methods";
            case "INSTRUCTION" -> "Instructions";
            default -> type;
        };
    }
}
