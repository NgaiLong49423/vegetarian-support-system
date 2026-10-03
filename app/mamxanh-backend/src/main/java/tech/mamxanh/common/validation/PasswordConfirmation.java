package tech.mamxanh.common.validation;

/** Request payload that carries a new password and its confirmation. */
public interface PasswordConfirmation {

    String password();

    String confirmPassword();
}
