package br.com.eric.usermanagement.security;

import br.com.eric.usermanagement.config.JwtConfig;
import br.com.eric.usermanagement.domain.entity.User;
import br.com.eric.usermanagement.domain.enums.Role;
import br.com.eric.usermanagement.domain.enums.UserStatus;
import br.com.eric.usermanagement.dto.TokenResponse;
import br.com.eric.usermanagement.service.JwtService;
import br.com.eric.usermanagement.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Usa as mesmas fábricas do JwtConfig, sem subir o Spring. */
class JwtServiceTest {

    private static final String SECRET = "test-only-secret-key-with-more-than-32-characters";

    private final JwtConfig config = new JwtConfig();
    private JwtEncoder encoder;
    private JwtDecoder decoder;
    private JwtService jwtService;

    private static SecurityProperties properties(String secret, long minutes) {
        return new SecurityProperties(
                new SecurityProperties.Jwt(secret, minutes),
                new SecurityProperties.Admin("Admin", "admin@teste.com", "Admin@123"));
    }

    @BeforeEach
    void setUp() {
        SecurityProperties properties = properties(SECRET, 5);
        SecretKey key = config.jwtSecretKey(properties);
        encoder = config.jwtEncoder(key);
        decoder = config.jwtDecoder(key);
        jwtService = new JwtService(encoder, properties);
    }

    private User admin() {
        User user = TestData.user("Admin", "admin@teste.com", Role.ADMIN, UserStatus.ACTIVE);
        user.setId(7L);
        return user;
    }

    @Test
    void generatesTokenWithExpectedClaims() {
        TokenResponse response = jwtService.generate(admin());

        Jwt jwt = decoder.decode(response.accessToken());

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(300);
        assertThat(jwt.getSubject()).isEqualTo("admin@teste.com");
        assertThat(jwt.<String>getClaim("role")).isEqualTo("ADMIN");
        assertThat(jwt.<Number>getClaim("userId").longValue()).isEqualTo(7L);
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        String token = jwtService.generate(admin()).accessToken();
        JwtDecoder otherDecoder = config.jwtDecoder(
                config.jwtSecretKey(properties("another-secret-key-with-more-than-32-characters", 5)));

        assertThatThrownBy(() -> otherDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("admin@teste.com")
                .issuedAt(now.minus(10, ChronoUnit.MINUTES))
                .expiresAt(now.minus(5, ChronoUnit.MINUTES))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }
}
