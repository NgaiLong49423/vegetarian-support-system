package tech.mamxanh.integration.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AI System Instructions and Behavioral Policy Loader Tests")
class AiPromptLoaderTest {
    @Test
    void formattingGuidanceIsLoadedWithoutChangingStructuredResponseContracts() {
        String prompt = new AiPromptLoader().loadSystemPrompt();
        assertThat(prompt).contains("Định dạng phần lời trả lời", "dòng trống", "Không xuất HTML",
                "Giữ nguyên schema JSON", "consent, confirmation");
    }


    private AiPromptLoader loader;

    @BeforeEach
    void setUp() {
        loader = new AiPromptLoader();
        loader.clearCache();
    }

    @Test
    @DisplayName("Should load agent operational instructions stripped of front matter")
    void shouldLoadAgentInstructions() {
        String instructions = loader.loadAgentInstructions();
        assertThat(instructions).isNotBlank();
        assertThat(instructions).doesNotStartWith("---");
        assertThat(instructions).contains("CHỈ DẪN VẬN HÀNH AGENT");
        assertThat(instructions).contains("create_shopping_list", "get_shopping_list");
        assertThat(instructions).contains("QA", "CONFIRM", "AUTO");
        assertThat(instructions).contains("PARTIAL_FAILURE");
    }

    @Test
    @DisplayName("Should successfully load and synthesize system.md with all four core behavioral policies")
    void shouldLoadSystemPromptWithAllPolicies() {
        String prompt = loader.loadSystemPrompt();

        assertThat(prompt).isNotBlank();
        // Identity & Core Role
        assertThat(prompt).contains("Mâm Xanh");
        assertThat(prompt).contains("Chỉ dẫn Nền tảng cho Trợ lý AI Mâm Xanh");
        assertThat(prompt).contains("Danh tính và Vai trò (Identity & Role)");
        assertThat(prompt).contains("Hành vi Tự chủ và Điều phối Công cụ (Agentic Behavior)");
        assertThat(prompt).contains("Khai thác Tri thức Nghiệp vụ (Knowledge Usage)");
        assertThat(prompt).contains("Bảo vệ Dữ liệu Phiên và Quyền Riêng tư (Session & Personal Data)");
        assertThat(prompt).contains("Thao tác Tác động và Cơ chế Xác nhận (Actions & Confirmation)");
        assertThat(prompt).contains("Quy chuẩn Hành vi và Văn phong Phản hồi (Response Behavior)");

        // Core Behavioral Policies Synthesized
        assertThat(prompt).contains("BEHAVIORAL POLICIES (MÂM XANH OPERATIONAL RULES)");
        assertThat(prompt).contains("POLICY: scope-policy");
        assertThat(prompt).contains("POLICY: safety-policy");
        assertThat(prompt).contains("POLICY: security-policy");
        assertThat(prompt).contains("POLICY: response-style");
    }

    @Test
    @DisplayName("Should load individual policy documents from classpath")
    void shouldLoadIndividualPoliciesDirectly() {
        String scopePolicy = loader.loadPolicy("scope-policy");
        assertThat(scopePolicy).contains("Chính sách Phạm vi Hoạt động của AI Assistant");
        assertThat(scopePolicy).contains("In-Scope Topics");
        assertThat(scopePolicy).contains("Out-of-Scope Topics");

        String safetyPolicy = loader.loadPolicy("safety-policy");
        assertThat(safetyPolicy).contains("Chính sách An toàn Nội dung và Ranh giới Y tế");
        assertThat(safetyPolicy).contains("Ranh giới Y tế và Dinh dưỡng Lâm sàng");
        assertThat(safetyPolicy).contains("SELF_HARM_OR_HARM");

        String securityPolicy = loader.loadPolicy("security-policy");
        assertThat(securityPolicy).contains("Chính sách Bảo mật, Chống Prompt Injection và Phân quyền");
        assertThat(securityPolicy).contains("Hierarchy of Authority");
        assertThat(securityPolicy).contains("Không Dựa vào Prompt để Thực thi Phân quyền");

        String responseStyle = loader.loadPolicy("response-style");
        assertThat(responseStyle).contains("Quy chuẩn Phong cách Phản hồi và Giao tiếp của AI");
        assertThat(responseStyle).contains("Tuyệt đối KHÔNG sử dụng Mã Kỹ thuật với Khách hàng");
    }

    @Test
    @DisplayName("Should load all core policies stripped of YAML front matter")
    void shouldLoadAllCorePoliciesStripped() {
        Map<String, String> policies = loader.loadAllCorePolicies();

        assertThat(policies).hasSize(4);
        assertThat(policies).containsKeys("scope-policy", "safety-policy", "security-policy", "response-style");

        for (Map.Entry<String, String> entry : policies.entrySet()) {
            assertThat(entry.getValue()).doesNotStartWith("---");
            assertThat(entry.getValue()).doesNotContain("category: ai-policy");
        }
    }

    @Test
    @DisplayName("Should correctly strip YAML front matter from markdown content")
    void shouldStripFrontMatterCorrectly() {
        String markdownWithFrontMatter = """
                ---
                id: test-id
                title: Test Title
                version: 1.0.0
                ---
                # Test Heading
                This is the actual markdown body.
                """;

        String stripped = AiPromptLoader.stripFrontMatter(markdownWithFrontMatter);

        assertThat(stripped).isEqualTo("""
                # Test Heading
                This is the actual markdown body.""".strip());
    }

    @Test
    @DisplayName("Should cache system prompt in memory and return cached instance")
    void shouldCachePromptsInMemory() {
        String firstCall = loader.loadSystemPrompt();
        String secondCall = loader.loadSystemPrompt();

        assertThat(secondCall).isSameAs(firstCall);
    }

    @Test
    @DisplayName("Should handle missing resource gracefully with empty string and fallback")
    void shouldFallbackSafelyWhenResourceMissing() {
        String nonExistent = loader.loadResource("ai/prompts/non-existent-prompt.md");
        assertThat(nonExistent).isEmpty();

        String nonExistentPolicy = loader.loadPolicy("unknown-policy-xyz");
        assertThat(nonExistentPolicy).isEmpty();
    }

    @Test
    @DisplayName("Should verify no API keys, credentials, or secrets leak into prompts")
    void shouldVerifyNoSecretOrApiKeyInPrompt() {
        String prompt = loader.loadSystemPrompt();

        assertThat(prompt).doesNotContain("AIzaSy"); // Google API key prefix
        assertThat(prompt).doesNotContain("sk-");
        assertThat(prompt).doesNotContain("bearer ");
        assertThat(prompt).doesNotContain("password=");
        assertThat(prompt).doesNotContain("secret=");
    }

    @Test
    @DisplayName("Should verify that Knowledge Base catalog or raw files are not dumped into system instruction")
    void shouldNotDumpRawKnowledgeBaseIntoSystemPrompt() {
        String prompt = loader.loadSystemPrompt();

        // System prompt contains operational guidelines, not the full 38-file knowledge base texts
        assertThat(prompt).doesNotContain("catalog.json");
        assertThat(prompt).doesNotContain("docs/requirements/srs/BUSINESS-RULES.md#br-09");
        assertThat(prompt.length()).isLessThan(45_000); // Bounded size for LLM context window efficiency
    }
}
