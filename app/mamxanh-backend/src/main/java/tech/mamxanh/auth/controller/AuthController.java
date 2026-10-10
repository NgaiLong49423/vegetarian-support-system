package tech.mamxanh.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import tech.mamxanh.auth.dto.request.EmailRequest;
import tech.mamxanh.auth.dto.request.LoginRequest;
import tech.mamxanh.auth.dto.request.PasswordResetConfirmRequest;
import tech.mamxanh.auth.dto.request.RegisterRequest;
import tech.mamxanh.auth.dto.request.TokenRequest;
import tech.mamxanh.auth.dto.response.AuthResponse;
import tech.mamxanh.auth.dto.response.MessageResponse;
import tech.mamxanh.auth.dto.response.RegistrationResponse;
import tech.mamxanh.auth.service.EmailVerificationService;
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
    private final PasswordResetService passwordResetService;

    public AuthController(RegistrationService registrationService,
            EmailVerificationService emailVerificationService, LoginService loginService,
            PasswordResetService passwordResetService) {
        this.registrationService = registrationService;
        this.emailVerificationService = emailVerificationService;
        this.loginService = loginService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(@Valid @RequestBody RegisterRequest request) {
        return registrationService.register(request);
    }

    @PostMapping("/email-verifications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody TokenRequest request) {
        emailVerificationService.verify(request.token());
    }

    @PostMapping("/email-verifications/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse resendVerificationEmail(@Valid @RequestBody EmailRequest request) {
        return emailVerificationService.resend(request.email());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return loginService.login(request);
    }

    @PostMapping("/password-resets")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse requestPasswordReset(@Valid @RequestBody EmailRequest request) {
        return passwordResetService.requestReset(request.email());
    }

    @PostMapping("/password-resets/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(request);
    }
}
