package br.com.eric.usermanagement.repository;

import br.com.eric.usermanagement.BaseIntegrationTest;
import br.com.eric.usermanagement.domain.entity.User;
import br.com.eric.usermanagement.domain.enums.Role;
import br.com.eric.usermanagement.domain.enums.UserStatus;
import br.com.eric.usermanagement.dto.UserFilter;
import br.com.eric.usermanagement.dto.UserSummaryResponse;
import br.com.eric.usermanagement.exception.InvalidQueryException;
import br.com.eric.usermanagement.support.TestData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes da listagem (Criteria API). Como o banco é compartilhado com outros testes, todos os dados
 * levam uma "tag" única no nome e no e-mail, e as buscas sempre filtram por ela.
 */
@Transactional
class UserSearchRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    private String tag;

    @BeforeEach
    void createData() {
        tag = UUID.randomUUID().toString().substring(0, 8);

        User ana = user("Ana", Role.USER, UserStatus.ACTIVE);
        ana.addAddress(TestData.address("Zeta", false));
        ana.addAddress(TestData.address("Campinas", true));

        User bruno = user("Bruno", Role.USER, UserStatus.INACTIVE);
        bruno.addAddress(TestData.address("Belém", true));

        User carla = user("Carla", Role.USER, UserStatus.ACTIVE);
        carla.addAddress(TestData.address("Santos", true));

        User dario = user("Dario", Role.USER, UserStatus.ACTIVE);
        dario.addAddress(TestData.address("Recife", true));
        dario.setDeleted(true);

        User eva = user("Eva", Role.ADMIN, UserStatus.ACTIVE);

        userRepository.saveAll(List.of(ana, bruno, carla, dario, eva));
        userRepository.flush();
        entityManager.clear();
    }

    private User user(String name, Role role, UserStatus status) {
        return TestData.user(name + " " + tag, name.toLowerCase() + "-" + tag + "@test.com", role, status);
    }

    private Page<UserSummaryResponse> search(UserFilter filter, Pageable pageable) {
        return userRepository.search(filter, pageable);
    }

    private Pageable byName() {
        return PageRequest.of(0, 10, Sort.by("name"));
    }

    private List<String> names(Page<UserSummaryResponse> page) {
        return page.getContent().stream().map(UserSummaryResponse::name).toList();
    }

    @Test
    void returnsActiveRecordsWithTheCityOfTheMainAddress() {
        Page<UserSummaryResponse> page = search(new UserFilter(tag, null, null, null), byName());

        assertThat(names(page)).containsExactly("Ana " + tag, "Bruno " + tag, "Carla " + tag, "Eva " + tag);
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getContent().get(0).city()).isEqualTo("Campinas");
        assertThat(page.getContent().get(0).state()).isEqualTo("SP");
        assertThat(page.getContent().get(3).city()).isNull();
    }

    @Test
    void searchMatchesNameOrEmailIgnoringCase() {
        assertThat(names(search(new UserFilter("BRUNO-" + tag, null, null, null), byName())))
                .containsExactly("Bruno " + tag);
        assertThat(names(search(new UserFilter("carla " + tag, null, null, null), byName())))
                .containsExactly("Carla " + tag);
    }

    @Test
    void filtersByTheCityOfTheMainAddressOnly() {
        assertThat(names(search(new UserFilter(tag, "camp", null, null), byName())))
                .containsExactly("Ana " + tag);
        assertThat(search(new UserFilter(tag, "zeta", null, null), byName()).getContent()).isEmpty();
    }

    @Test
    void filtersByStatusAndRole() {
        assertThat(names(search(new UserFilter(tag, null, UserStatus.INACTIVE, null), byName())))
                .containsExactly("Bruno " + tag);
        assertThat(names(search(new UserFilter(tag, null, null, Role.ADMIN), byName())))
                .containsExactly("Eva " + tag);
    }

    @Test
    void sortsByTheCityOfTheMainAddress() {
        Pageable cityDesc = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "city"));

        Page<UserSummaryResponse> page = search(new UserFilter(tag, null, null, Role.USER), cityDesc);

        assertThat(names(page)).containsExactly("Carla " + tag, "Ana " + tag, "Bruno " + tag);
    }

    @Test
    void paginatesWithoutDuplicatingUsersThatHaveSeveralAddresses() {
        UserFilter filter = new UserFilter(tag, null, null, Role.USER);

        Page<UserSummaryResponse> first = search(filter, PageRequest.of(0, 2, Sort.by("name")));
        Page<UserSummaryResponse> second = search(filter, PageRequest.of(1, 2, Sort.by("name")));

        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(names(first)).containsExactly("Ana " + tag, "Bruno " + tag);
        assertThat(names(second)).containsExactly("Carla " + tag);
        assertThat(second.isLast()).isTrue();
    }

    @Test
    void rejectsSortFieldsOutsideTheWhitelist() {
        Pageable invalid = PageRequest.of(0, 10, Sort.by("password"));

        assertThatThrownBy(() -> search(new UserFilter(tag, null, null, null), invalid))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("password");
    }

    @Test
    void treatsPercentSignAsLiteralText() {
        Page<UserSummaryResponse> page = search(new UserFilter("%", null, null, null), byName());

        assertThat(page.getContent()).isEmpty();
    }
}
