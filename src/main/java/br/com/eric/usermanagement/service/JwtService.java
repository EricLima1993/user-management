package br.com.eric.usermanagement.service;

import br.com.eric.usermanagement.domain.entity.User;
import br.com.eric.usermanagement.dto.TokenResponse;
import br.com.eric.usermanagement.security.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;

    public TokenResponse generate(User user) {
        Instant now = Instant.now();
        long minutes = properties.jwt().expirationMinutes();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("user-management-api")
                .subject(user.getEmail())
                .issuedAt(now)
                .expiresAt(now.plus(minutes, ChronoUnit.MINUTES))
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenResponse(token, "Bearer", minutes * 60);
    }
}
