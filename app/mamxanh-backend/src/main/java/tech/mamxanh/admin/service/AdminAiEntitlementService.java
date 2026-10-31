package tech.mamxanh.admin.service;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.admin.dto.AdminAiCreditGrantResponse;
import tech.mamxanh.admin.dto.AdminAiPackageResponse;
import tech.mamxanh.admin.dto.GiftAiPackageRequest;
import tech.mamxanh.admin.dto.GrantAiCreditRequest;
import tech.mamxanh.admin.repository.AdminUserAuditRepository;
import tech.mamxanh.admin.repository.AdminUserQueryRepository;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.notification.service.NotificationService;
import tech.mamxanh.subscription.service.AdminSubscriptionService;
import tech.mamxanh.subscription.service.AiCreditAdministrationService;

/** Administrator-only operations that change one account's AI entitlement or Credit wallet. */
@Service
public class AdminAiEntitlementService {
    private final AdminUserQueryRepository users;
    private final AdminUserAuditRepository audits;
    private final AdminSubscriptionService subscriptions;
    private final AiCreditAdministrationService credits;
    private final NotificationService notifications;
    private final CurrentUserService currentUser;

    public AdminAiEntitlementService(AdminUserQueryRepository users, AdminUserAuditRepository audits,
            AdminSubscriptionService subscriptions, AiCreditAdministrationService credits,
            NotificationService notifications, CurrentUserService currentUser) {
        this.users = users;
        this.audits = audits;
        this.subscriptions = subscriptions;
        this.credits = credits;
        this.notifications = notifications;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public AdminAiPackageResponse currentTier(long userId) {
        requireUser(userId);
        return response(subscriptions.currentTier(userId));
    }

    @Transactional
    public AdminAiPackageResponse giftPackage(long userId, GiftAiPackageRequest request) {
        String reason = normalizedReason(request.reason());
        var change = subscriptions.giftPackage(userId, request.tier());
        if (change.changed()) {
            audits.recordAiPackageGift(userId, currentUser.requireAuthenticatedUserId(), change.oldTier(), change.newTier(),
                    change.endsAt(), reason, change.changedAt());
            notifications.aiPackageGifted(userId, change.newTier(), change.endsAt(), reason);
        }
        return response(change);
    }

    @Transactional
    public AdminAiCreditGrantResponse grantCredit(long userId, GrantAiCreditRequest request) {
        requireUser(userId);
        String reason = normalizedReason(request.reason());
        var grant = credits.grant(userId, currentUser.requireAuthenticatedUserId(), request.credits(), reason);
        notifications.aiCreditGranted(userId, grant.credits(), grant.reason());
        return new AdminAiCreditGrantResponse(userId, grant.credits(), grant.balance(), grant.grantedAt());
    }

    private void requireUser(long userId) {
        if (users.findById(userId).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy tài khoản.");
        }
    }

    private static String normalizedReason(String reason) {
        String value = reason == null ? "" : reason.strip();
        if (value.length() < 10 || value.length() > 500) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    "Lý do phải có từ 10 đến 500 ký tự.");
        }
        return value;
    }

    private static AdminAiPackageResponse response(AdminSubscriptionService.TierChange change) {
        return new AdminAiPackageResponse(change.userId(), change.oldTier(), change.newTier(),
                change.startsAt(), change.endsAt(), change.tokenQuota(), change.changed(), change.changedAt());
    }
}
