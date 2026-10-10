package br.com.eric.usermanagement.service;

import br.com.eric.usermanagement.domain.entity.User;
import br.com.eric.usermanagement.domain.enums.Role;
import br.com.eric.usermanagement.domain.enums.UserStatus;
import br.com.eric.usermanagement.dto.LoginRequest;
import br.com.eric.usermanagement.dto.TokenResponse;
import br.com.eric.usermanagement.exception.InvalidCredentialsException;
import br.com.eric.usermanagement.repository.UserRepository;
import br.com.eric.usermanagement.support.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User activeUser() {
        return TestData.user("Maria", "maria@teste.com", Role.USER, UserStatus.ACTIVE);
    }

    @Test
    void returnsTokenWhenCredentialsAreValid() {
        User user = activeUser();
        TokenResponse token = new TokenResponse("jwt", "Bearer", 300);
        when(userRepository.findByEmail("maria@teste.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Senha@1234", "hash-de-teste")).thenReturn(true);
        when(jwtService.generate(user)).thenReturn(token);

        TokenResponse result = authService.login(new LoginRequest("  Maria@Teste.com ", "Senha@1234"));

        assertThat(result).isSameAs(token);
    }

    @Test
    void rejectsUnknownEmail() {
        when(userRepository.findByEmail("ninguem@teste.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ninguem@teste.com", "qualquer")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtService, never()).generate(any(User.class));
    }

    @Test
    void rejectsWrongPassword() {
        when(userRepository.findByEmail("maria@teste.com")).thenReturn(Optional.of(activeUser()));
        when(passwordEncoder.matches("errada", "hash-de-teste")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("maria@teste.com", "errada")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtService, never()).generate(any(User.class));
    }

    @Test
    void rejectsInactiveUserEvenWithCorrectPassword() {
        User user = activeUser();
        user.setStatus(UserStatus.INACTIVE);
        when(userRepository.findByEmail("maria@teste.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Senha@1234", "hash-de-teste")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("maria@teste.com", "Senha@1234")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtService, never()).generate(any(User.class));
    }
}
