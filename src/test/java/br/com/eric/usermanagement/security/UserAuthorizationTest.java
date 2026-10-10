package br.com.eric.usermanagement.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserAuthorizationTest {

    private final UserAuthorization authorization = new UserAuthorization();

    private JwtAuthenticationToken tokenWithUserId(Object userId) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("maria@teste.com")
                .claim("userId", userId)
                .build();
        return new JwtAuthenticationToken(jwt);
    }

    @Test
    void isSelfWhenIdMatchesTokenUserId() {
        assertThat(authorization.isSelf(5L, tokenWithUserId(5L))).isTrue();
    }

    @Test
    void isSelfWorksWhenClaimIsAnInteger() {
        assertThat(authorization.isSelf(5L, tokenWithUserId(5))).isTrue();
    }

    @Test
    void isNotSelfWhenIdDiffers() {
        assertThat(authorization.isSelf(6L, tokenWithUserId(5L))).isFalse();
    }

    @Test
    void isNotSelfWhenIdIsNull() {
        assertThat(authorization.isSelf(null, tokenWithUserId(5L))).isFalse();
    }

    @Test
    void isNotSelfWhenClaimIsNotANumber() {
        assertThat(authorization.isSelf(5L, tokenWithUserId("5"))).isFalse();
    }

    @Test
    void isNotSelfWhenAuthenticationIsNotJwt() {
        var authentication = new UsernamePasswordAuthenticationToken("maria", "x", List.of());

        assertThat(authorization.isSelf(5L, authentication)).isFalse();
    }
}
