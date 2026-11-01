package tech.mamxanh.subscription.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.subscription.dto.response.AdminTransactionReport;
import tech.mamxanh.subscription.dto.response.AdminTransactionResponse;
import tech.mamxanh.subscription.repository.TransactionAuditRepository;
import tech.mamxanh.subscription.repository.TransactionAuditRepository.Filter;

@Service
@Transactional(readOnly = true)
public class TransactionAuditService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int EXPORT_LIMIT = 10_000;
    private final TransactionAuditRepository repository;
    private final Clock clock;
    public TransactionAuditService(TransactionAuditRepository repository, Clock clock) { this.repository = repository; this.clock = clock; }

    public AdminTransactionReport list(String query, String tier, String status, LocalDate from, LocalDate to, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw invalid("Trang phải từ 0, kích thước trang từ 1 đến 100.");
        Filter filter = filter(query, tier, status, from, to);
        var summary = repository.summary(filter);
        return new AdminTransactionReport(PageResponse.of(repository.page(filter, (long) page * size, size),
                page, size, summary.total()), summary);
    }

    public AdminTransactionResponse detail(long id) {
        if (id < 1) throw invalid("Mã giao dịch không hợp lệ.");
        return repository.find(id).orElseThrow(() -> new AppException(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));
    }

    public String export(String query, String tier, String status, LocalDate from, LocalDate to) {
        var rows = repository.page(filter(query, tier, status, from, to), 0, EXPORT_LIMIT + 1);
        if (rows.size() > EXPORT_LIMIT) throw invalid("Tối đa 10.000 giao dịch mỗi lần xuất. Hãy thu hẹp bộ lọc.");
        StringBuilder csv = new StringBuilder("\uFEFFMã giao dịch,Mã đơn payOS,Khách hàng,Email,Gói,Số tiền VND,Trạng thái,Tạo lúc UTC,Thanh toán lúc UTC,Mã đăng ký,Hết hạn UTC\r\n");
        for (var row : rows) {
            Object[] values = {"TX-" + row.id(), row.orderCode(), row.customerName(), row.customerEmail(), row.tier(),
                    row.amountVnd(), row.status(), row.createdAt(), row.paidAt(), row.subscriptionId(), row.subscriptionEndsAt()};
            for (int i = 0; i < values.length; i++) {
                if (i > 0) csv.append(',');
                csv.append(cell(values[i]));
            }
            csv.append("\r\n");
        }
        return csv.toString();
    }

    private Filter filter(String query, String tier, String status, LocalDate from, LocalDate to) {
        String search = blank(query), plan = blank(tier), state = blank(status);
        if (search != null && (search.length() > 200 || search.codePoints().anyMatch(Character::isISOControl)))
            throw invalid("Từ khóa tối đa 200 ký tự và không chứa ký tự điều khiển.");
        if (plan != null && !Set.of("PLUS", "PRO").contains(plan)
                || state != null && !Set.of("PENDING", "PAID", "FAILED", "CANCELLED").contains(state))
            throw invalid("Gói hoặc trạng thái giao dịch không hợp lệ.");
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZONE);
        if (from != null && (from.getYear() < 1900 || from.isAfter(today))
                || to != null && (to.getYear() < 1900 || to.isAfter(today))
                || from != null && to != null && from.isAfter(to))
            throw invalid("Khoảng ngày không hợp lệ hoặc nằm trong tương lai.");
        return new Filter(search, plan, state,
                from == null ? null : from.atStartOfDay(ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
                to == null ? null : to.plusDays(1).atStartOfDay(ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
    }

    private static String blank(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    private static AppException invalid(String message) { return new AppException(ErrorCode.VALIDATION_FAILED, message); }
    private static String cell(Object value) {
        String text = value == null ? "" : value.toString();
        String leading = text.stripLeading();
        if (!leading.isEmpty() && ("=+-@".indexOf(leading.charAt(0)) >= 0 || Character.isISOControl(leading.charAt(0)))
                || text.startsWith("\t") || text.startsWith("\r") || text.startsWith("\n")) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
