package tech.mamxanh.auth.controller;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import tech.mamxanh.auth.dto.response.FollowMemberResponse;
import tech.mamxanh.auth.dto.response.FollowStatusResponse;
import tech.mamxanh.auth.service.MemberFollowService;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.common.response.PageResponse;

/** FR-59 following between Members: public summary (UC-59.3), follow/unfollow and lists (UC-59.1, 59.2, 59.4). */
@RestController
@RequestMapping(path = "/api/v1/members/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class MemberFollowController {
    private final MemberFollowService service;

    public MemberFollowController(MemberFollowService service) {
        this.service = service;
    }

    @GetMapping("/follow")
    @Operation(summary = "Read a member's follow summary",
            description = "Public. Counts include only active accounts. With an access token, `following` tells whether the viewer follows this member.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Follow summary of the member profile."),
            @ApiResponse(responseCode = "404", description = "MEMBER_PROFILE_NOT_FOUND: no member profile has this id.")
    })
    public FollowStatusResponse status(@PathVariable @Min(1) long userId) {
        return service.getStatus(userId);
    }

    @PutMapping("/follow")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Follow a member",
            description = "Idempotent. The follower is the authenticated account; a repeated request keeps one relation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The current account follows the member."),
            @ApiResponse(responseCode = "401", description = "Authentication is required."),
            @ApiResponse(responseCode = "403", description = "MEMBER_ACCESS_REQUIRED for a non-member account; FOLLOW_SELF_NOT_ALLOWED on the own profile."),
            @ApiResponse(responseCode = "404", description = "MEMBER_PROFILE_NOT_FOUND: no member profile has this id."),
            @ApiResponse(responseCode = "409", description = "FOLLOW_TARGET_INACTIVE: the member account is locked.")
    })
    public FollowStatusResponse follow(@PathVariable @Min(1) long userId) {
        return service.follow(userId);
    }

    @DeleteMapping("/follow")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Unfollow a member",
            description = "Idempotent. Removes only the authenticated account's relation, also when the member is locked.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The current account does not follow the member."),
            @ApiResponse(responseCode = "401", description = "Authentication is required."),
            @ApiResponse(responseCode = "403", description = "MEMBER_ACCESS_REQUIRED for a non-member account; FOLLOW_SELF_NOT_ALLOWED on the own profile."),
            @ApiResponse(responseCode = "404", description = "MEMBER_PROFILE_NOT_FOUND: no member profile has this id.")
    })
    public FollowStatusResponse unfollow(@PathVariable @Min(1) long userId) {
        return service.unfollow(userId);
    }

    @GetMapping("/followers")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "List a member's followers",
            description = "Active accounts only, most recently followed first, with public profile fields only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of followers."),
            @ApiResponse(responseCode = "400", description = "Page or size is outside its supported range."),
            @ApiResponse(responseCode = "401", description = "Authentication is required."),
            @ApiResponse(responseCode = "404", description = "MEMBER_PROFILE_NOT_FOUND: no member profile has this id.")
    })
    public PageResponse<FollowMemberResponse> followers(@PathVariable @Min(1) long userId,
            @Parameter(description = "Zero-based page index.") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, from 1 through 50.") @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.followers(userId, page, size);
    }

    @GetMapping("/following")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "List the members a member follows",
            description = "Active accounts only, most recently followed first, with public profile fields only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of followed members."),
            @ApiResponse(responseCode = "400", description = "Page or size is outside its supported range."),
            @ApiResponse(responseCode = "401", description = "Authentication is required."),
            @ApiResponse(responseCode = "404", description = "MEMBER_PROFILE_NOT_FOUND: no member profile has this id.")
    })
    public PageResponse<FollowMemberResponse> following(@PathVariable @Min(1) long userId,
            @Parameter(description = "Zero-based page index.") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, from 1 through 50.") @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.following(userId, page, size);
    }
}
