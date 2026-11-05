package tech.mamxanh.integration.ai.knowledge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tech.mamxanh.integration.ai.AiPromptLoader;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Knowledge Base and AI Policies Vietnamese UTF-8 Encoding & Mojibake Integrity Tests")
class KnowledgeEncodingIntegrityTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Regex patterns for common Mojibake and encoding corruption:
     * 1. \uFFFD (Unicode replacement character)
     * 2. Double-encoded UTF-8 sequences (Ã followed by Latin-1 supplement bytes)
     * 3. Specific corruptions from misread code pages (trA, kA with soft hyphens/control chars, etc.)
     */
    private static final Pattern MOJIBAKE_PATTERN = Pattern.compile(
            "\\uFFFD|" +
            "\\u00C3[\\u0080-\\u00BF]|" +
            "\\u00C4[\\u0080-\\u00BF]|" +
            "\\u00C5[\\u0080-\\u00BF]|" +
            "\\u00C6[\\u0080-\\u00BF]|" +
            "á»[\\u0080-\\u00BF]|" +
            "áº[\\u0080-\\u00BF]|" +
            "trA[\\x00-\\x1F\\x7F-\\x9F]|" +
            "kA[\\x00-\\x1F\\x7F-\\x9F]|" +
            "\\u00C2\\u00A0" // non-breaking space artifact
    );

    @Test
    @DisplayName("Should detect no Mojibake in all Knowledge Base and AI Policy files")
    void shouldDetectNoMojibakeInAnyKnowledgeOrAiFile() throws IOException {
        List<Path> rootDirs = List.of(
                Path.of("src/main/resources/knowledge-base"),
                Path.of("src/main/resources/ai")
        );

        List<String> violations = new ArrayList<>();
        int fileCount = 0;

        for (Path rootDir : rootDirs) {
            if (!Files.exists(rootDir)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(rootDir)) {
                List<Path> files = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".md") || p.toString().endsWith(".json"))
                        .toList();

                for (Path file : files) {
                    fileCount++;
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    Matcher matcher = MOJIBAKE_PATTERN.matcher(content);
                    if (matcher.find()) {
                        violations.add(file.getFileName().toString() + " matched: " + matcher.group());
                    }
                }
            }
        }

        assertThat(fileCount).isGreaterThanOrEqualTo(40);
        assertThat(violations)
                .withFailMessage("Found Mojibake encoding artifacts in files: %s", violations)
                .isEmpty();
    }

    @Test
    @DisplayName("Should verify catalog.json has valid JSON, proper count, and valid Vietnamese titles")
    void shouldVerifyCatalogJsonIntegrityAndDiacritics() throws IOException {
        ClassPathResource catalogResource = new ClassPathResource("knowledge-base/catalog.json");
        assertThat(catalogResource.exists()).isTrue();

        String jsonText;
        try (InputStream in = catalogResource.getInputStream()) {
            jsonText = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        JsonNode root = MAPPER.readTree(jsonText);
        assertThat(root.has("documents")).isTrue();
        JsonNode docs = root.get("documents");
        assertThat(docs.isArray()).isTrue();

        int totalDocs = root.get("totalDocuments").asInt();
        assertThat(docs.size()).isEqualTo(totalDocs);

        for (JsonNode doc : docs) {
            String id = doc.get("id").asText();
            String title = doc.get("title").asText();
            String resourcePath = doc.get("resourcePath").asText();
            String category = doc.get("category").asText();
            String status = doc.get("implementationStatus").asText();

            assertThat(id).isNotBlank().matches("[a-z0-9-]+");
            assertThat(resourcePath).isNotBlank().startsWith("knowledge-base/");
            assertThat(category).isNotBlank();
            assertThat(status).isEqualTo("verified");

            // Verify title has no mojibake and has valid text
            assertThat(title).isNotBlank();
            assertThat(MOJIBAKE_PATTERN.matcher(title).find())
                    .withFailMessage("Title for %s contains Mojibake: %s", id, title)
                    .isFalse();

            // Verify specific known diacritical titles
            if (id.equals("expert-registration")) {
                assertThat(title).isEqualTo("Quy trình đăng ký trở thành Chuyên gia ẩm thực chay");
            } else if (id.equals("ai-chat-cooking-guidance")) {
                assertThat(title).isEqualTo("Hỏi đáp ẩm thực chay và tư vấn nấu nướng cùng AI");
            } else if (id.equals("registration-and-login")) {
                assertThat(title).isEqualTo("Đăng ký, đăng nhập và bảo mật tài khoản");
            }
        }
    }

    @Test
    @DisplayName("Should verify AI system prompt and behavioral policies load with correct Vietnamese characters")
    void shouldVerifyAiPromptLoaderVietnameseContent() {
        AiPromptLoader loader = new AiPromptLoader();
        loader.clearCache();

        String systemPrompt = loader.loadSystemPrompt();
        assertThat(systemPrompt).isNotBlank();

        // Check key Vietnamese phrases in prompt & policies
        assertThat(systemPrompt).contains("Mâm Xanh");
        assertThat(systemPrompt).contains("Chỉ dẫn Nền tảng cho Trợ lý AI Mâm Xanh");
        assertThat(systemPrompt).contains("Chính sách Phạm vi Hoạt động của AI Assistant");
        assertThat(systemPrompt).contains("Chính sách An toàn Nội dung và Ranh giới Y tế");
        assertThat(systemPrompt).contains("Chính sách Bảo mật, Chống Prompt Injection và Phân quyền");
        assertThat(systemPrompt).contains("Quy chuẩn Phong cách Phản hồi và Giao tiếp của AI");

        // Verify no replacement characters or mojibake in generated prompt
        assertThat(systemPrompt).doesNotContain("\uFFFD");
        assertThat(MOJIBAKE_PATTERN.matcher(systemPrompt).find()).isFalse();
    }
}
