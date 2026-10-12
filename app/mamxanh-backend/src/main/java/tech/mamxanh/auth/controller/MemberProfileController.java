package tech.mamxanh.auth.controller;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import tech.mamxanh.auth.dto.request.UpdateProfileRequest;
import tech.mamxanh.auth.dto.response.MemberProfileResponse;
import tech.mamxanh.auth.service.MemberProfileService;
import tech.mamxanh.common.config.OpenApiConfig;

/** FR-23 member profiles: public read (UC-23.2) and own profile settings (UC-23.3). */
@RestController
@RequestMapping("/api/v1")
@Validated
public class MemberProfileController {

    private final MemberProfileService service;

    public MemberProfileController(MemberProfileService service) {
        this.service = service;
    }

    @GetMapping("/members/{userId}")
    public MemberProfileResponse publicProfile(@PathVariable @Min(1) long userId) {
        return service.getPublicProfile(userId);
    }

    @GetMapping("/me/profile")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public MemberProfileResponse ownProfile() {
        return service.getOwnProfile();
    }

    @PutMapping("/me/profile")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public MemberProfileResponse updateOwnProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return service.updateOwnProfile(request);
    }

    @PostMapping(value = "/me/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public MemberProfileResponse uploadAvatar(@RequestParam("file") MultipartFile file) {
        return service.uploadAvatar(file);
    }
}
