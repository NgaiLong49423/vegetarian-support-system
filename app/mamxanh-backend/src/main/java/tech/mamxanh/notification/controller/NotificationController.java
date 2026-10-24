package tech.mamxanh.notification.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.common.response.PageResponse;
import tech.mamxanh.notification.dto.request.NotificationPreferencesRequest;
import tech.mamxanh.notification.dto.response.NotificationPreferencesResponse;
import tech.mamxanh.notification.dto.response.NotificationResponse;
import tech.mamxanh.notification.dto.response.UnreadNotificationCountResponse;
import tech.mamxanh.notification.service.NotificationService;

/** FR-49 notification center of the signed-in user (UC-49.1, UC-49.2) and email preference (UC-49.4). */
@RestController
@RequestMapping(path = "/api/v1/me", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@Validated
public class NotificationController {
    private final NotificationService service;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/notifications")
    @Operation(summary = "List own notifications",
            description = "Newest first. Listing does not mark notifications as read.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of the caller's notifications."),
            @ApiResponse(responseCode = "400", description = "Page or size is outside its supported range."),
            @ApiResponse(responseCode = "401", description = "Authentication is required.")
    })
    public PageResponse<NotificationResponse> list(
            @Parameter(description = "Zero-based page index.") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, from 1 through 50.") @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @Parameter(description = "Only unread notifications.") @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return service.list(currentUserService.requireAuthenticatedUserId(), page, size, unreadOnly);
    }

    @GetMapping("/notifications/unread-count")
    @Operation(summary = "Count own unread notifications", description = "The number shown on the notification bell.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unread notification count."),
            @ApiResponse(responseCode = "401", description = "Authentication is required.")
    })
    public UnreadNotificationCountResponse unreadCount() {
        return new UnreadNotificationCountResponse(service.unreadCount(currentUserService.requireAuthenticatedUserId()));
    }

    @PutMapping("/notifications/{notificationId}/read")
    @Operation(summary = "Mark one own notification as read", description = "Idempotent.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The notification, now read."),
            @ApiResponse(responseCode = "401", description = "Authentication is required."),
            @ApiResponse(responseCode = "404", description = "NOTIFICATION_NOT_FOUND: no notification of the caller has this id.")
    })
    public NotificationResponse markRead(@PathVariable @Min(1) long notificationId) {
        return service.markRead(currentUserService.requireAuthenticatedUserId(), notificationId);
    }

    @PutMapping("/notifications/read-all")
    @Operation(summary = "Mark all own notifications as read")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Every notification of the caller is read."),
            @ApiResponse(responseCode = "401", description = "Authentication is required.")
    })
    public ResponseEntity<Void> markAllRead() {
        service.markAllRead(currentUserService.requireAuthenticatedUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notification-preferences")
    @Operation(summary = "Read own email notification preference")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email preference."),
            @ApiResponse(responseCode = "401", description = "Authentication is required.")
    })
    public NotificationPreferencesResponse preferences() {
        return service.preferences(currentUserService.requireAuthenticatedUserId());
    }

    @PutMapping(path = "/notification-preferences", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Turn reply emails on or off",
            description = "In-app notifications are always created; only the email channel follows this preference.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The saved email preference."),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: replyEmailEnabled is required."),
            @ApiResponse(responseCode = "401", description = "Authentication is required.")
    })
    public NotificationPreferencesResponse updatePreferences(@Valid @RequestBody NotificationPreferencesRequest request) {
        return service.updatePreferences(currentUserService.requireAuthenticatedUserId(), request.replyEmailEnabled());
    }
}
