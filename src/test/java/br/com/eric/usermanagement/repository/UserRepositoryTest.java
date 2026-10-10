package br.com.eric.usermanagement.repository;

import br.com.eric.usermanagement.BaseIntegrationTest;
import br.com.eric.usermanagement.domain.entity.User;
import br.com.eric.usermanagement.domain.enums.Role;
import br.com.eric.usermanagement.domain.enums.UserStatus;
import br.com.eric.usermanagement.support.TestData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de repositório contra o H2 (modo PostgreSQL), com o schema criado pelo Flyway.
 * Cada teste roda em uma transação que sofre rollback ao final.
 */
@Transactional
class UserRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    private String email() {
        return "repo-" + UUID.randomUUID() + "@test.com";
    }

    private User newUser(String email) {
        return TestData.user("Maria", email, Role.USER, UserStatus.ACTIVE);
    }

    @Test
    void savePopulatesAuditFields() {
        User saved = userRepository.saveAndFlush(newUser(email()));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("system");
        assertThat(saved.isDeleted()).isFalse();
    }

    @Test
    void findByEmailFindsActiveUser() {
        String email = email();
        userRepository.saveAndFlush(newUser(email));
        entityManager.clear();

        assertThat(userRepository.findByEmail(email)).isPresent();
    }

    @Test
    void deletedUsersAreHiddenFromQueries() {
        String email = email();
        User user = userRepository.saveAndFlush(newUser(email));
        user.setDeleted(true);
        userRepository.flush();
        entityManager.clear();

        assertThat(userRepository.findByEmail(email)).isEmpty();
        assertThat(userRepository.findById(user.getId())).isEmpty();
    }

    @Test
    void existsIncludingDeletedSeesDeletedUsers() {
        String email = email();
        User user = userRepository.saveAndFlush(newUser(email));
        user.setDeleted(true);
        userRepository.flush();
        entityManager.clear();

        assertThat(userRepository.existsByEmailIncludingDeleted(email)).isTrue();
        assertThat(userRepository.existsByEmailIncludingDeleted(email())).isFalse();
    }

    @Test
    void emailIsUniqueInTheDatabase() {
        String email = email();
        userRepository.saveAndFlush(newUser(email));

        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser(email)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
