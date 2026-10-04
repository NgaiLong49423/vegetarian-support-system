package tech.mamxanh.auth.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.entity.AccountStatus;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.common.exception.ApiException;

/** Account role changes remain owned by auth and are exposed to business modules as a service. */
@Service
public class ExpertPromotionService {
    private final UserRepository users;
    private final Clock clock;

    public ExpertPromotionService(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public void promoteActiveCustomer(long userId) {
        var user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "EXPERT_APPLICATION_ACCOUNT_INELIGIBLE", "Tài khoản không còn đủ điều kiện được phê duyệt."));
        if (user.getAccountStatus() != AccountStatus.ACTIVE || user.getRole() != Role.CUSTOMER) {
            throw new ApiException(HttpStatus.CONFLICT, "EXPERT_APPLICATION_ACCOUNT_INELIGIBLE", "Tài khoản phải đang ACTIVE và có vai trò CUSTOMER để được phê duyệt.");
        }
        user.promoteToExpert(LocalDateTime.now(clock.withZone(ZoneOffset.UTC)));
    }

    @Transactional(readOnly = true)
    public void requireActiveCustomer(long userId) {
        var user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "EXPERT_APPLICATION_CUSTOMER_REQUIRED", "Chỉ Customer có tài khoản ACTIVE được nộp đơn."));
        if (user.getAccountStatus() != AccountStatus.ACTIVE || user.getRole() != Role.CUSTOMER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EXPERT_APPLICATION_CUSTOMER_REQUIRED", "Chỉ Customer có tài khoản ACTIVE được nộp đơn.");
        }
    }
}
