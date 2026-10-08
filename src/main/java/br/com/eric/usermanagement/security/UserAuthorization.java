package br.com.eric.usermanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("userAuthorization")
public class UserAuthorization {

    public boolean isSelf(Long id, Authentication authentication) {
        if (id != null && authentication instanceof JwtAuthenticationToken jwt) {
            Object userId = jwt.getToken().getClaim("userId");
            return userId instanceof Number number && number.longValue() == id;
        }
        return false;
    }
}
