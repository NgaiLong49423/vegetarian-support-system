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
            assertThat(source.uri()).startsWith("docs/requirements/");
        });
    }

    @Test
    void returnsNoSourceWhenApprovedCorpusHasNoMatchingEvidence() {
        assertThat(retriever.retrieve("quần đảo ngoài hành tinh" )).isEmpty();
    }
}
