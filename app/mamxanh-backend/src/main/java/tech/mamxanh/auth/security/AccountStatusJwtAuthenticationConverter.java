package tech.mamxanh.auth.security;

import java.util.List;
import java.util.Optional;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import tech.mamxanh.auth.entity.AccountStatus;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;

/**
 * Runs after the Resource Server has verified the token signature, issuer and expiry. Every
 * authenticated request re-reads {@code USER} (decision Q20/NFR-09) so an administrative lock
 * takes effect immediately: {@code LOCKED} is rejected with 403 {@code ACCOUNT_LOCKED} through
 * {@link LockedException}. Authorities come from the same row, so the current {@code USER.role}
 * applies even while an older token is still valid; the token's {@code role} claim is informational.
 */
@Component
public class AccountStatusJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    public AccountStatusJwtAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        User user = parseUserId(jwt.getSubject())
                .flatMap(userRepository::findById)
                .orElseThrow(() -> new InvalidBearerTokenException("Access token subject is not an existing account"));
        if (user.getAccountStatus() == AccountStatus.LOCKED) {
            throw new LockedException("Account is locked");
        }
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private static Optional<Long> parseUserId(String subject) {
        if (subject == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(subject));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
