package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApprovedAiKnowledgeRetrieverTest {
    private final ApprovedAiKnowledgeRetriever retriever = new ApprovedAiKnowledgeRetriever();

    @Test
    void returnsApprovedSourcesForMatchingEvidenceAndNoInternetSource() {
        var sources = retriever.retrieve("consent hồ sơ sức khỏe Gemini xác nhận AI");
        assertThat(sources).isNotEmpty().allSatisfy(source -> {
            assertThat(source.type()).isEqualTo("APPROVED_DOCUMENT");
            assertThat(source.uri()).matches("(?:knowledge-base/|docs/requirements/).*");
        });
    }

    @Test
    void retrievesPackagedHelpInsteadOfOnlyHardcodedPolicySnippets() {
        assertThat(retriever.retrieve("Quên mật khẩu và khôi phục tài khoản"))
                .extracting(source -> source.id()).contains("password-recovery");
        assertThat(retriever.retrieve("So sánh các gói dịch vụ AI Free Plus Pro"))
                .extracting(source -> source.id()).contains("plans-overview");
    }

    @Test
    void returnsNoSourceWhenApprovedCorpusHasNoMatchingEvidence() {
        assertThat(retriever.retrieve("quần đảo ngoài hành tinh" )).isEmpty();
    }
}
