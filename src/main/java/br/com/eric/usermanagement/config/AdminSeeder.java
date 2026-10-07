package br.com.eric.usermanagement.config;

import br.com.eric.usermanagement.domain.entity.User;
import br.com.eric.usermanagement.domain.enums.Role;
import br.com.eric.usermanagement.domain.enums.UserStatus;
import br.com.eric.usermanagement.repository.UserRepository;
import br.com.eric.usermanagement.security.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        SecurityProperties.Admin admin = properties.admin();
        if (userRepository.findByEmail(admin.email()).isPresent()) {
            return;
        }

        User user = new User();
        user.setName(admin.name());
        user.setEmail(admin.email());
        user.setPassword(passwordEncoder.encode(admin.password()));
        user.setRole(Role.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        log.info("ADMIN inicial criado: {}", admin.email());
    }
}
