package tech.mamxanh.expertapplication.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.service.ExpertPromotionService;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.expertapplication.dto.ExpertApplicationRequest;
import tech.mamxanh.expertapplication.dto.ExpertApplicationResponse;
import tech.mamxanh.expertapplication.repository.ExpertApplicationRepository;
import tech.mamxanh.notification.service.NotificationService;

@Service
public class ExpertApplicationService {
    private final ExpertApplicationRepository applications;
    private final ExpertPromotionService promotion;
    private final NotificationService notifications;
    private final Clock clock;

    public ExpertApplicationService(ExpertApplicationRepository applications,
            ExpertPromotionService promotion, NotificationService notifications, Clock clock) {
        this.applications = applications; this.promotion = promotion;
        this.notifications = notifications; this.clock = clock;
    }

    @Transactional
    public ExpertApplicationResponse submit(ExpertApplicationRequest request) {
        long userId = userId();
        promotion.requireActiveCustomer(userId);
        try {
            long id = applications.insert(userId, request, now());
            return applications.findById(id);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "EXPERT_APPLICATION_PENDING_EXISTS", "Bạn đã có đơn đang chờ xét duyệt.");
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpertApplicationResponse> ownHistory(int page, int size) {
        long userId = userId();
        return PageResponse.of(applications.page(userId, null, page, size), page, size, applications.count(userId, null));
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpertApplicationResponse> adminList(String status, int page, int size) {
        return PageResponse.of(applications.page(null, status, page, size), page, size, applications.count(null, status));
    }

    @Transactional(readOnly = true)
    public ExpertApplicationResponse adminDetail(long id) { return applications.findById(id); }

    @Transactional
    public ExpertApplicationResponse approve(long id, String note) {
        long reviewer = userId();
        LocalDateTime now = now();
        if (applications.transitionPending(id, "APPROVED", note, reviewer, now) == 0) throw stale();
        long applicantId = applications.applicantId(id);
        promotion.promoteActiveCustomer(applicantId);
        notifications.expertApplicationDecision(applicantId, id, true, null);
        return applications.findById(id);
    }

    @Transactional
    public ExpertApplicationResponse reject(long id, String reason) {
        long reviewer = userId();
        LocalDateTime now = now();
        String cleanReason = reason.trim();
        if (applications.transitionPending(id, "REJECTED", cleanReason, reviewer, now) == 0) throw stale();
        long applicantId = applications.applicantId(id);
        notifications.expertApplicationDecision(applicantId, id, false, cleanReason);
        return applications.findById(id);
    }

    private long userId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        try { return Long.parseLong(authentication.getName()); }
        catch (RuntimeException exception) { throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Cần đăng nhập."); }
    }
    private LocalDateTime now() { return LocalDateTime.now(clock.withZone(ZoneOffset.UTC)); }
    private static ApiException stale() { return new ApiException(HttpStatus.CONFLICT, "EXPERT_APPLICATION_STALE", "Đơn đã được xử lý hoặc không còn chờ duyệt."); }
}
