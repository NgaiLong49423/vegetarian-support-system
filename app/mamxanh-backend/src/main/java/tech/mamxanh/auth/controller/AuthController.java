package tech.mamxanh.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;
import tech.mamxanh.auth.dto.request.EmailRequest;
import tech.mamxanh.auth.dto.request.GoogleLoginRequest;
import tech.mamxanh.auth.dto.request.LoginRequest;
import tech.mamxanh.auth.dto.request.PasswordResetConfirmRequest;
import tech.mamxanh.auth.dto.request.RegisterRequest;
import tech.mamxanh.auth.dto.request.TokenRequest;
import tech.mamxanh.auth.dto.response.AuthResponse;
import tech.mamxanh.auth.dto.response.MessageResponse;
import tech.mamxanh.auth.dto.response.RegistrationResponse;
import tech.mamxanh.auth.service.EmailVerificationService;
import tech.mamxanh.auth.service.GoogleLoginService;
import tech.mamxanh.auth.service.LoginService;
import tech.mamxanh.auth.service.PasswordResetService;
import tech.mamxanh.auth.service.RegistrationService;

/** FR-03 authentication endpoints; springdoc generates their runtime OpenAPI contract. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final EmailVerificationService emailVerificationService;
    private final LoginService loginService;
    private final GoogleLoginService googleLoginService;
    private final PasswordResetService passwordResetService;

    public AuthController(RegistrationService registrationService,
            EmailVerificationService emailVerificationService, LoginService loginService,
            GoogleLoginService googleLoginService, PasswordResetService passwordResetService) {
        this.registrationService = registrationService;
        this.emailVerificationService = emailVerificationService;
        this.loginService = loginService;
        this.googleLoginService = googleLoginService;
        this.passwordResetService = passwordResetService;
    }

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created; a verification email is sent."),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: field errors in `errors`."),
            @ApiResponse(responseCode = "409", description = "EMAIL_ALREADY_USED: an account already uses this email.")
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(@Valid @RequestBody RegisterRequest request) {
        return registrationService.register(request);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Email verified."),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED, or VERIFICATION_TOKEN_INVALID for an unknown, used, replaced or expired token.")
    })
    @PostMapping("/email-verifications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody TokenRequest request) {
        emailVerificationService.verify(request.token());
    }

    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Same neutral answer whether or not the email has an unverified account."),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: field errors in `errors`."),
            @ApiResponse(responseCode = "429", description = "RESEND_TOO_SOON: within 60 seconds of the previous email; see Retry-After.")
    })
    @PostMapping("/email-verifications/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse resendVerificationEmail(@Valid @RequestBody EmailRequest request) {
        return emailVerificationService.resend(request.email());
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access token and account summary."),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: field errors in `errors`."),
            @ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS: wrong email or password, or a Google-only account."),
            @ApiResponse(responseCode = "403", description = "EMAIL_NOT_VERIFIED or ACCOUNT_LOCKED."),
            @ApiResponse(responseCode = "429", description = "LOGIN_TEMPORARILY_BLOCKED: five wrong passwords in a row; see Retry-After.")
    })
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return loginService.login(request);
    }

    /** UC-03.5: exchanges a Google ID Token from Google Identity Services for an access token. */
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access token and account summary."),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: field errors in `errors`."),
            @ApiResponse(responseCode = "401", description = "GOOGLE_TOKEN_INVALID: the Google ID token cannot be verified."),
            @ApiResponse(responseCode = "403", description = "ACCOUNT_LOCKED."),
            @ApiResponse(responseCode = "409", description = "GOOGLE_ACCOUNT_CONFLICT: the email is linked to another Google account."),
            @ApiResponse(responseCode = "503", description = "GOOGLE_LOGIN_UNAVAILABLE: Google signing keys cannot be loaded.")
    })
    @PostMapping("/google")
    public AuthResponse googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return googleLoginService.login(request.idToken());
    }

    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Same neutral answer whether or not the email has an account."),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: field errors in `errors`.")
    })
    @PostMapping("/password-resets")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse requestPasswordReset(@Valid @RequestBody EmailRequest request) {
        return passwordResetService.requestReset(request.email());
    }

    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed."),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED, PASSWORD_RESET_TOKEN_INVALID or NEW_PASSWORD_SAME_AS_CURRENT."),
            @ApiResponse(responseCode = "403", description = "ACCOUNT_LOCKED: the account was locked after the email was sent.")
    })
    @PostMapping("/password-resets/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(request);
    }
}
