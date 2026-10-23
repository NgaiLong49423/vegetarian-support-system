package tech.mamxanh.admin.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tech.mamxanh.admin.dto.AiTelemetryReport.Totals;
import tech.mamxanh.admin.dto.AiCostEstimate;
import tech.mamxanh.integration.ai.AiTokenUsage;
import tech.mamxanh.integration.ai.AiUsageEvent;

/** Parameterized SQL Server operations for technical logs; no cross-module repository access. */
@Repository
public class AiTelemetryRepository {
    private final JdbcTemplate jdbc;
    public AiTelemetryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Daily(LocalDate day, String feature, Totals totals) { }

    public void insert(AiUsageEvent event, AiTokenUsage usage, String status, AiCostEstimate estimate) {
        // The indexed key-range lock serializes re-delivery across application instances.
        jdbc.update("""
            INSERT INTO AI_USAGE_TELEMETRY
              (event_id,user_id,anonymous_session_id,feature,model,service_tier,occurred_at,
               prompt_tokens,response_tokens,total_tokens,thinking_tokens,cached_tokens,tool_use_prompt_tokens,
               usage_status,pricing_version,estimated_cost_usd)
            SELECT ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,? WHERE NOT EXISTS
              (SELECT 1 FROM AI_USAGE_TELEMETRY WITH (UPDLOCK,HOLDLOCK) WHERE event_id=?)
            """, event.eventId().toString(), event.userId(), event.anonymousSessionId(), event.feature(),
                usage.model(), usage.serviceTier(), utc(event.occurredAt()),
                usage.promptTokens(), usage.responseTokens(), usage.totalTokens(), usage.thinkingTokens(),
                usage.cachedTokens(), usage.toolUsePromptTokens(), status,
                estimate == null ? null : estimate.version(), estimate == null ? null : estimate.usd(),
                event.eventId().toString());
    }

    public int deleteBatchBefore(Instant cutoff) {
        return jdbc.update("DELETE TOP (1000) FROM AI_USAGE_TELEMETRY WHERE occurred_at < ?", utc(cutoff));
    }

    public List<Daily> daily(Instant from, Instant to, String feature) {
        return jdbc.query("""
            SELECT CONVERT(date,DATEADD(MINUTE,420,occurred_at)) AS bucket_day, feature,
              COUNT_BIG(*) AS calls, SUM(prompt_tokens) AS prompt_tokens, SUM(response_tokens) AS response_tokens,
              SUM(total_tokens) AS total_tokens, SUM(COALESCE(thinking_tokens,0)) AS thinking_tokens,
              SUM(COALESCE(cached_tokens,0)) AS cached_tokens,
              SUM(CAST(CASE WHEN usage_status <> 'COMPLETE' THEN 1 ELSE 0 END AS BIGINT)) AS incomplete,
              SUM(CAST(CASE WHEN estimated_cost_usd IS NULL THEN 1 ELSE 0 END AS BIGINT)) AS unpriced,
              COALESCE(SUM(estimated_cost_usd),0) AS known_cost
            FROM AI_USAGE_TELEMETRY WHERE occurred_at >= ? AND occurred_at < ? AND (? IS NULL OR feature = ?)
            GROUP BY CONVERT(date,DATEADD(MINUTE,420,occurred_at)),feature ORDER BY bucket_day,feature
            """, (rs, row) -> {
                long unpriced = rs.getLong("unpriced");
                var known = rs.getBigDecimal("known_cost");
                return new Daily(rs.getDate("bucket_day").toLocalDate(), rs.getString("feature"),
                        new Totals(rs.getLong("calls"), rs.getLong("prompt_tokens"), rs.getLong("response_tokens"),
                                rs.getLong("total_tokens"), rs.getLong("thinking_tokens"), rs.getLong("cached_tokens"),
                                rs.getLong("incomplete"), unpriced, known, unpriced == 0 ? known : null));
            }, utc(from), utc(to), feature, feature);
    }

    // DATETIME2 has no timezone. java.sql.Timestamp would use the JVM's default timezone in JDBC.
    private static LocalDateTime utc(Instant value) { return LocalDateTime.ofInstant(value, ZoneOffset.UTC); }
}
