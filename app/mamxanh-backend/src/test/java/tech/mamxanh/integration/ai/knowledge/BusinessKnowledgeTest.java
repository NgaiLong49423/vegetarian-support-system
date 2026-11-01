package tech.mamxanh.integration.ai.knowledge;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import static org.assertj.core.api.Assertions.*;

class BusinessKnowledgeTest {
    @Test
    void runtimeBudgetContinuesAuthorizedIndexLedgerWithoutResettingSpend(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toString()
                .equals(tech.mamxanh.integration.ai.adk.PilotLiveBudget.PRICING_DATE), "Pricing re-verification required on another date");
        var json = new tools.jackson.databind.ObjectMapper();
        var file = dir.resolve("budget.json");
        var before = new tech.mamxanh.integration.ai.adk.PilotLiveBudget.State(.65,471,3,
                tech.mamxanh.integration.ai.adk.PilotLiveBudget.PRICING_DATE);
        java.nio.file.Files.writeString(file,json.writeValueAsString(before));
        var budget = tech.mamxanh.integration.ai.adk.PilotLiveBudget.configured(file.toString());
        budget.generation(0);
        var after = json.readValue(java.nio.file.Files.readString(file),tech.mamxanh.integration.ai.adk.PilotLiveBudget.State.class);
        assertThat(after.embeddingAttempts()).isEqualTo(471);
        assertThat(after.generationAttempts()).isEqualTo(4);
        assertThat(after.reservedUpperUsd()).isGreaterThan(.65);
        java.nio.file.Files.writeString(file,json.writeValueAsString(new tech.mamxanh.integration.ai.adk.PilotLiveBudget.State(
                4.999,471,4,tech.mamxanh.integration.ai.adk.PilotLiveBudget.PRICING_DATE)));
        assertThatThrownBy(() -> budget.generation(0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(json.readValue(java.nio.file.Files.readString(file),tech.mamxanh.integration.ai.adk.PilotLiveBudget.State.class)
                .reservedUpperUsd()).isEqualTo(4.999);
    }

    @Test
    void loadsAllRegisteredSourcesAndKeepsPlaybooks() {
        var business = KnowledgeCatalog.business();
        assertThat(business.documents()).hasSize(38);
        assertThat(business.documents()).allSatisfy(d -> {
            assertThat(d.content()).doesNotStartWith("---");
            assertThat(d.metadata().sources()).allMatch(s -> s.startsWith("knowledge-base/"));
        });
        assertThat(KnowledgeCatalog.agent().document("shopping-create")).isNotNull();
        assertThat(KnowledgeCatalog.agent().document("password-recovery")).isNotNull();
    }

    @Test
    void retrievesRelevantSourcesAndRejectsArbitraryPaths() {
        try (var retrieval = new HybridKnowledgeRetriever(KnowledgeCatalog.agent(), null, null, Clock.systemUTC())) {
            var result = retrieval.search("Quên mật khẩu và khôi phục tài khoản");
            assertThat(result.status()).isEqualTo("keyword_degraded");
            assertThat(result.hits()).extracting(HybridKnowledgeRetriever.Hit::knowledgeId).contains("password-recovery");
            assertThat(retrieval.get("password-recovery").content()).contains("/quen-mat-khau");
            assertThatThrownBy(() -> retrieval.get("../../application.properties")).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
