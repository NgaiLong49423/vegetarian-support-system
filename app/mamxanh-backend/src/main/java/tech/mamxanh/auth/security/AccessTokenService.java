package tech.mamxanh.auth.security;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import tech.mamxanh.auth.entity.User;

/**
 * Issues Stateless JWT access tokens (API.md section 3.1). Claims: {@code iss}, {@code sub} (the
 * {@code USER.user_id}), {@code role}, {@code iat}, {@code exp}. There is no {@code sid} because
 * no server session exists, and no refresh token.
 */
@Service
public class AccessTokenService {

    public static final String ROLE_CLAIM = "role";

    private final JwtEncoder jwtEncoder;
    private final AuthProperties authProperties;
    private final Clock clock;

    public AccessTokenService(JwtEncoder jwtEncoder, AuthProperties authProperties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.authProperties = authProperties;
        this.clock = clock;
    }

    public IssuedAccessToken issue(User user) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(authProperties.jwtIssuer())
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(authProperties.accessTokenTtl()))
                .claim(ROLE_CLAIM, user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(token, authProperties.accessTokenTtl().toSeconds());
    }

    /** {@code value} is a bearer credential: never log it. */
    public record IssuedAccessToken(String value, long expiresInSeconds) {

        @Override
        public String toString() {
            return "IssuedAccessToken[redacted, expiresInSeconds=" + expiresInSeconds + "]";
        }
    }
}
