package br.com.eric.usermanagement.service;

import br.com.eric.usermanagement.domain.entity.User;
import br.com.eric.usermanagement.domain.enums.UserStatus;
import br.com.eric.usermanagement.dto.LoginRequest;
import br.com.eric.usermanagement.dto.TokenResponse;
import br.com.eric.usermanagement.exception.InvalidCredentialsException;
import br.com.eric.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public TokenResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);

        boolean validPassword = passwordEncoder.matches(request.password(), user.getPassword());
        if (!validPassword || user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException();
        }
        return jwtService.generate(user);
    }
}