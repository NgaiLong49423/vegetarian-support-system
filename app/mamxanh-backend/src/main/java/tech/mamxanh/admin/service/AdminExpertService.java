package tech.mamxanh.admin.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.admin.dto.AdminExpertDetailResponse;
import tech.mamxanh.admin.repository.AdminUserQueryRepository;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.expertapplication.service.ExpertApplicationService;

@Service
public class AdminExpertService {
    private final AdminUserQueryRepository users;
    private final ExpertApplicationService applications;

    public AdminExpertService(AdminUserQueryRepository users, ExpertApplicationService applications) {
        this.users = users;
        this.applications = applications;
    }

    @Transactional(readOnly = true)
    public AdminExpertDetailResponse detail(long userId) {
        AdminExpertDetailResponse profile = users.findExpertById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EXPERT_NOT_FOUND",
                        "Không tìm thấy tài khoản Chuyên gia."));
        return new AdminExpertDetailResponse(profile.account(), profile.avatarUrl(), profile.bio(),
                applications.adminLatestApprovedForUser(userId));
    }
}
