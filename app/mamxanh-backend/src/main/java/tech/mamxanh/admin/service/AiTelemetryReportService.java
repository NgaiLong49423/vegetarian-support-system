package tech.mamxanh.admin.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.admin.dto.AiTelemetryReport;
import tech.mamxanh.admin.dto.AiTelemetryReport.Bucket;
import tech.mamxanh.admin.dto.AiTelemetryReport.Totals;
import tech.mamxanh.admin.repository.AiTelemetryRepository;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

@Service
public class AiTelemetryReportService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final AiTelemetryRepository repository;
    private final Clock clock;
    public AiTelemetryReportService(AiTelemetryRepository repository, Clock clock) { this.repository = repository; this.clock = clock; }
    private record Key(LocalDate period, String feature) { }

    @Transactional(readOnly = true)
    public AiTelemetryReport report(LocalDate from, LocalDate to, String granularity, String feature) {
        var today = LocalDate.ofInstant(clock.instant(), ZONE);
        if (from == null || to == null || from.isAfter(to) || to.isAfter(today)
                || from.isBefore(today.minusDays(90)) || !java.util.Set.of("DAY", "WEEK", "MONTH").contains(granularity)
                || feature != null && !AiTelemetryWriter.FEATURES.contains(feature)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Khoảng ngày tối đa 90 ngày gần nhất; kiểm tra ngày, chu kỳ và tính năng.");
        }
        var retainedFrom = clock.instant().minus(Duration.ofDays(90));
        var start = from.atStartOfDay(ZONE).toInstant();
        if (start.isBefore(retainedFrom)) start = retainedFrom;
        var end = to.plusDays(1).atStartOfDay(ZONE).toInstant();
        Map<Key, Totals> grouped = new LinkedHashMap<>();
        var total = zero();
        for (var row : repository.daily(start, end, feature)) {
            LocalDate period = switch (granularity) {
                case "WEEK" -> row.day().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                case "MONTH" -> row.day().withDayOfMonth(1);
                default -> row.day();
            };
            grouped.merge(new Key(period, row.feature()), row.totals(), AiTelemetryReportService::add);
            total = add(total, row.totals());
        }
        var buckets = new ArrayList<Bucket>();
        grouped.forEach((key, value) -> buckets.add(new Bucket(key.period(), key.feature(), value)));
        return new AiTelemetryReport(from, to, granularity, ZONE.getId(), retainedFrom, "USD",
                "STANDARD_PAID_EQUIVALENT", total, buckets);
    }

    static Totals zero() { return new Totals(0,0,0,0,0,0,0,0,BigDecimal.ZERO,BigDecimal.ZERO); }
    static Totals add(Totals a, Totals b) {
        long unpriced = a.unpricedCalls() + b.unpricedCalls();
        var cost = a.knownEstimatedCostUsd().add(b.knownEstimatedCostUsd());
        return new Totals(a.calls()+b.calls(), a.promptTokens()+b.promptTokens(), a.responseTokens()+b.responseTokens(),
                a.totalTokens()+b.totalTokens(), a.thinkingTokens()+b.thinkingTokens(), a.cachedTokens()+b.cachedTokens(),
                a.incompleteUsageCalls()+b.incompleteUsageCalls(), unpriced, cost, unpriced == 0 ? cost : null);
    }
}
