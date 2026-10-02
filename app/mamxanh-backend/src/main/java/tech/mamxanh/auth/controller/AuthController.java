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
import tech.mamxanh.auth.dto.request.RegisterRequest;
import tech.mamxanh.auth.dto.request.TokenRequest;
import tech.mamxanh.auth.dto.response.AuthResponse;
import tech.mamxanh.auth.dto.response.MessageResponse;
import tech.mamxanh.auth.dto.response.RegistrationResponse;
import tech.mamxanh.auth.service.EmailVerificationService;
import tech.mamxanh.auth.service.LoginService;
import tech.mamxanh.auth.service.RegistrationService;

/** FR-03 authentication endpoints (docs/api/openapi.yaml, tag Authentication). */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final EmailVerificationService emailVerificationService;
    private final LoginService loginService;

    public AuthController(RegistrationService registrationService,
            EmailVerificationService emailVerificationService, LoginService loginService) {
        this.registrationService = registrationService;
        this.emailVerificationService = emailVerificationService;
        this.loginService = loginService;
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
}
