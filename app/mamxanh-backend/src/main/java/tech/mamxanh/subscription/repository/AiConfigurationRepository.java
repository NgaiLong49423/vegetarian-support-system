package tech.mamxanh.subscription.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.subscription.entity.AiConfigurationSnapshot;
import tech.mamxanh.subscription.entity.AiConfigurationSnapshot.Credit;
import tech.mamxanh.subscription.entity.AiConfigurationSnapshot.Model;
import tech.mamxanh.subscription.entity.AiConfigurationSnapshot.Plan;

/** SQL Server persistence for the Admin-managed AI configuration. */
@Repository
public class AiConfigurationRepository {
    private final JdbcTemplate jdbc;

    public AiConfigurationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long currentFreeQuota(Instant now) {
        LocalDateTime utcNow = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        Long quota = jdbc.queryForObject("""
                SELECT CASE WHEN pending_effective_at IS NOT NULL AND pending_effective_at <= ?
                            THEN pending_token_quota ELSE token_quota END
                FROM AI_PLAN_CONFIGURATION WHERE plan_tier = 'FREE'
                """, Long.class, utcNow);
        return quota == null ? 0 : quota;
    }

    @Transactional(readOnly = true)
    public AiConfigurationSnapshot findConfiguration() {
        List<Plan> plans = jdbc.query("""
                SELECT plan_tier, price_vnd, token_quota, pending_token_quota, pending_effective_at
                FROM AI_PLAN_CONFIGURATION ORDER BY CASE plan_tier WHEN 'FREE' THEN 0 WHEN 'PLUS' THEN 1 ELSE 2 END
                """, (rs, row) -> {
            LocalDateTime pendingAt = rs.getObject("pending_effective_at", LocalDateTime.class);
            return new Plan(rs.getString("plan_tier"), rs.getInt("price_vnd"), rs.getLong("token_quota"),
                    rs.getObject("pending_token_quota", Long.class), pendingAt == null ? null : pendingAt.toInstant(ZoneOffset.UTC));
        });
        List<Model> models = jdbc.query("""
                SELECT model_id, display_name, enabled, free_allowed, free_default,
                       plus_allowed, plus_default, pro_allowed, pro_default
                FROM AI_GEMINI_MODEL_CONFIGURATION ORDER BY display_name, model_id
                """, (rs, row) -> new Model(rs.getString("model_id"), rs.getString("display_name"),
                rs.getBoolean("enabled"), rs.getBoolean("free_allowed"), rs.getBoolean("free_default"),
                rs.getBoolean("plus_allowed"), rs.getBoolean("plus_default"),
                rs.getBoolean("pro_allowed"), rs.getBoolean("pro_default")));
        Credit credit = jdbc.queryForObject("""
                SELECT credits_per_1000_vnd, credits_per_1000_overage_tokens,
                       minimum_top_up_vnd, maximum_top_up_vnd
                FROM AI_CREDIT_CONFIGURATION WHERE configuration_id = 1
                """, (rs, row) -> new Credit(rs.getBigDecimal("credits_per_1000_vnd"),
                rs.getLong("credits_per_1000_overage_tokens"), rs.getInt("minimum_top_up_vnd"),
                rs.getInt("maximum_top_up_vnd")));
        return new AiConfigurationSnapshot(plans, models, credit);
    }

    public void promoteFreeQuotaIfDue(Instant now) {
        LocalDateTime utcNow = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        jdbc.update("""
                UPDATE AI_PLAN_CONFIGURATION
                SET token_quota = pending_token_quota, pending_token_quota = NULL,
                    pending_effective_at = NULL, updated_at = ?
                WHERE plan_tier = 'FREE' AND pending_effective_at IS NOT NULL AND pending_effective_at <= ?
                """, utcNow, utcNow);
    }

    public void updatePlan(Plan plan) {
        jdbc.update("""
                UPDATE AI_PLAN_CONFIGURATION
                SET price_vnd = ?, token_quota = ?, pending_token_quota = ?, pending_effective_at = ?, updated_at = ?
                WHERE plan_tier = ?
                """, plan.priceVnd(), plan.tokenQuota(), plan.pendingTokenQuota(), utc(plan.pendingEffectiveAt()),
                LocalDateTime.now(ZoneOffset.UTC), plan.tier());
    }

    public void updateCredit(Credit credit) {
        jdbc.update("""
                UPDATE AI_CREDIT_CONFIGURATION
                SET credits_per_1000_vnd = ?, credits_per_1000_overage_tokens = ?,
                    minimum_top_up_vnd = ?, maximum_top_up_vnd = ?, updated_at = ?
                WHERE configuration_id = 1
                """, credit.creditsPer1000Vnd(), credit.creditsPer1000OverageTokens(),
                credit.minimumTopUpVnd(), credit.maximumTopUpVnd(), LocalDateTime.now(ZoneOffset.UTC));
    }

    public void replaceModels(List<Model> models) {
        String placeholders = String.join(",", java.util.Collections.nCopies(models.size(), "?"));
        Object[] ids = models.stream().map(Model::modelId).toArray();
        jdbc.update("UPDATE AI_GEMINI_MODEL_CONFIGURATION SET enabled=0, free_allowed=0, free_default=0, "
                + "plus_allowed=0, plus_default=0, pro_allowed=0, pro_default=0, updated_at=? WHERE model_id NOT IN ("
                + placeholders + ")", concat(LocalDateTime.now(ZoneOffset.UTC), ids));
        for (Model model : models) {
            int updated = jdbc.update("""
                    UPDATE AI_GEMINI_MODEL_CONFIGURATION SET display_name=?, enabled=?, free_allowed=?, free_default=?,
                        plus_allowed=?, plus_default=?, pro_allowed=?, pro_default=?, updated_at=? WHERE model_id=?
                    """, model.displayName(), model.enabled(), model.freeAllowed(), model.freeDefault(),
                    model.plusAllowed(), model.plusDefault(), model.proAllowed(), model.proDefault(),
                    LocalDateTime.now(ZoneOffset.UTC), model.modelId());
            if (updated == 0) {
                jdbc.update("""
                        INSERT INTO AI_GEMINI_MODEL_CONFIGURATION
                            (model_id, display_name, enabled, free_allowed, free_default,
                             plus_allowed, plus_default, pro_allowed, pro_default, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, model.modelId(), model.displayName(), model.enabled(), model.freeAllowed(),
                        model.freeDefault(), model.plusAllowed(), model.plusDefault(), model.proAllowed(),
                        model.proDefault(), LocalDateTime.now(ZoneOffset.UTC));
            }
        }
    }

    private static LocalDateTime utc(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    private static Object[] concat(Object first, Object[] rest) {
        Object[] result = new Object[rest.length + 1];
        result[0] = first;
        System.arraycopy(rest, 0, result, 1, rest.length);
        return result;
    }
}
