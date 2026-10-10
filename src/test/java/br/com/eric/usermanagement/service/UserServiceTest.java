package br.com.eric.usermanagement.service;

import br.com.eric.usermanagement.domain.entity.Address;
import br.com.eric.usermanagement.domain.entity.User;
import br.com.eric.usermanagement.domain.enums.Role;
import br.com.eric.usermanagement.domain.enums.UserStatus;
import br.com.eric.usermanagement.dto.AddressRequest;
import br.com.eric.usermanagement.dto.AddressResponse;
import br.com.eric.usermanagement.dto.PageResponse;
import br.com.eric.usermanagement.dto.UserCreateRequest;
import br.com.eric.usermanagement.dto.UserFilter;
import br.com.eric.usermanagement.dto.UserResponse;
import br.com.eric.usermanagement.dto.UserSummaryResponse;
import br.com.eric.usermanagement.dto.UserUpdateRequest;
import br.com.eric.usermanagement.exception.BusinessRuleException;
import br.com.eric.usermanagement.exception.EmailAlreadyExistsException;
import br.com.eric.usermanagement.exception.InvalidCepException;
import br.com.eric.usermanagement.exception.ResourceNotFoundException;
import br.com.eric.usermanagement.mapper.AddressMapper;
import br.com.eric.usermanagement.mapper.AddressMapperImpl;
import br.com.eric.usermanagement.mapper.UserMapper;
import br.com.eric.usermanagement.mapper.UserMapperImpl;
import br.com.eric.usermanagement.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CepValidationService cepValidationService;

    // Mappers reais (gerados pelo MapStruct), observados pelo Mockito para a injeção no construtor
    @Spy
    private AddressMapper addressMapper = new AddressMapperImpl();
    @Spy
    private UserMapper userMapper = newUserMapper();

    @InjectMocks
    private UserService service;

    private static UserMapper newUserMapper() {
        UserMapperImpl mapper = new UserMapperImpl();
        ReflectionTestUtils.setField(mapper, "addressMapper", new AddressMapperImpl());
        return mapper;
    }

    // ---------- dados de apoio ----------

    private AddressRequest addressRequest(Long id, String number, boolean main) {
        return new AddressRequest(id, "01001000", "Praça da Sé", number, null, "SP", "São Paulo", "Sé", main);
    }

    private Address address(Long id, String number, boolean main) {
        Address address = new Address();
        address.setId(id);
        address.setCep("01001000");
        address.setStreet("Praça da Sé");
        address.setNumber(number);
        address.setState("SP");
        address.setCity("São Paulo");
        address.setDistrict("Sé");
        address.setMainAddress(main);
        return address;
    }

    private User user(Long id, Role role, Address... addresses) {
        User user = new User();
        user.setId(id);
        user.setName("Maria");
        user.setEmail("maria@teste.com");
        user.setPassword("hash");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        for (Address address : addresses) {
            user.addAddress(address);
        }
        return user;
    }

    private UserCreateRequest createRequest(String email, AddressRequest... addresses) {
        return new UserCreateRequest("Maria", email, "11999990000", "Senha@1234", Role.USER, List.of(addresses));
    }

    private UserUpdateRequest updateRequest(String name, String password, Role role, UserStatus status,
                                            AddressRequest... addresses) {
        return new UserUpdateRequest(name, "11999990000", password, role, status, List.of(addresses));
    }

    // ---------- criação ----------

    @Test
    void createHashesPasswordNormalizesEmailAndActivatesUser() {
        when(userRepository.existsByEmailIncludingDeleted("maria@teste.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha@1234")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = service.create(createRequest("  Maria@Teste.com ", addressRequest(null, "10", true)));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("maria@teste.com");
        assertThat(saved.getPassword()).isEqualTo("HASH");
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getAddresses()).hasSize(1);
        assertThat(saved.getAddresses().get(0).getUser()).isSameAs(saved);
        assertThat(response.email()).isEqualTo("maria@teste.com");
        verify(cepValidationService).validateAndEnrich(any(Address.class));
    }

    @Test
    void createRejectsDuplicatedEmail() {
        when(userRepository.existsByEmailIncludingDeleted("maria@teste.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(createRequest("maria@teste.com", addressRequest(null, "10", true))))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(cepValidationService);
    }

    @Test
    void createRejectsMoreThanOneMainAddress() {
        when(userRepository.existsByEmailIncludingDeleted("maria@teste.com")).thenReturn(false);

        assertThatThrownBy(() -> service.create(createRequest("maria@teste.com",
                addressRequest(null, "10", true), addressRequest(null, "20", true))))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createMakesFirstAddressMainWhenNoneIsMarked() {
        when(userRepository.existsByEmailIncludingDeleted("maria@teste.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(createRequest("maria@teste.com",
                addressRequest(null, "10", false), addressRequest(null, "20", false)));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        List<Address> addresses = captor.getValue().getAddresses();
        assertThat(addresses.get(0).isMainAddress()).isTrue();
        assertThat(addresses.get(1).isMainAddress()).isFalse();
    }

    @Test
    void createStopsWhenCepIsInvalid() {
        when(userRepository.existsByEmailIncludingDeleted("maria@teste.com")).thenReturn(false);
        doThrow(new InvalidCepException("01001000")).when(cepValidationService).validateAndEnrich(any(Address.class));

        assertThatThrownBy(() -> service.create(createRequest("maria@teste.com", addressRequest(null, "10", true))))
                .isInstanceOf(InvalidCepException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    // ---------- consulta ----------

    @Test
    void findByIdThrowsWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByIdReturnsOnlyActiveAddresses() {
        Address deleted = address(11L, "11", false);
        deleted.setDeleted(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, Role.USER, address(10L, "10", true), deleted)));

        UserResponse response = service.findById(2L);

        assertThat(response.addresses()).extracting(AddressResponse::number).containsExactly("10");
    }

    @Test
    void searchDelegatesToRepositoryAndWrapsThePage() {
        Pageable pageable = PageRequest.of(0, 10);
        UserFilter filter = new UserFilter("maria", null, null, null);
        UserSummaryResponse item = new UserSummaryResponse(
                1L, "Maria", "m@teste.com", null, Role.USER, UserStatus.ACTIVE, "Santos", "SP");
        when(userRepository.search(filter, pageable)).thenReturn(new PageImpl<>(List.of(item), pageable, 1));

        PageResponse<UserSummaryResponse> result = service.search(filter, pageable);

        assertThat(result.content()).containsExactly(item);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isTrue();
    }

    // ---------- atualização: permissões ----------

    @Test
    void userCannotChangeOwnRole() {
        User user = user(2L, Role.USER, address(10L, "10", true));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        UserUpdateRequest request = updateRequest("Maria", null, Role.ADMIN, null, addressRequest(10L, "10", true));

        assertThatThrownBy(() -> service.update(2L, request, false)).isInstanceOf(AccessDeniedException.class);

        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void userCannotChangeOwnStatus() {
        User user = user(2L, Role.USER, address(10L, "10", true));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        UserUpdateRequest request = updateRequest("Maria", null, null, UserStatus.INACTIVE, addressRequest(10L, "10", true));

        assertThatThrownBy(() -> service.update(2L, request, false)).isInstanceOf(AccessDeniedException.class);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void userMayResendTheSameRoleAndStatus() {
        User user = user(2L, Role.USER, address(10L, "10", true));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        UserResponse response = service.update(2L,
                updateRequest("Maria Nova", null, Role.USER, UserStatus.ACTIVE, addressRequest(10L, "10", true)), false);

        assertThat(response.name()).isEqualTo("Maria Nova");
    }

    @Test
    void adminCanChangeRoleAndStatus() {
        User user = user(2L, Role.USER, address(10L, "10", true));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        UserResponse response = service.update(2L,
                updateRequest("Maria", null, Role.ADMIN, UserStatus.INACTIVE, addressRequest(10L, "10", true)), true);

        assertThat(response.role()).isEqualTo(Role.ADMIN);
        assertThat(response.status()).isEqualTo(UserStatus.INACTIVE);
    }

    // ---------- atualização: senha ----------

    @Test
    void updateKeepsPasswordWhenNoneIsProvided() {
        User user = user(2L, Role.USER, address(10L, "10", true));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        service.update(2L, updateRequest("Maria", null, null, null, addressRequest(10L, "10", true)), false);

        verify(passwordEncoder, never()).encode(any());
        assertThat(user.getPassword()).isEqualTo("hash");
    }

    @Test
    void updateHashesNewPassword() {
        User user = user(2L, Role.USER, address(10L, "10", true));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NovaSenha@1")).thenReturn("NOVO-HASH");

        service.update(2L, updateRequest("Maria", "NovaSenha@1", null, null, addressRequest(10L, "10", true)), false);

        assertThat(user.getPassword()).isEqualTo("NOVO-HASH");
    }

    // ---------- atualização: endereços ----------

    @Test
    void updateCreatesUpdatesAndSoftDeletesAddresses() {
        Address kept = address(10L, "10", true);
        Address removed = address(11L, "11", false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, Role.USER, kept, removed)));

        UserResponse response = service.update(2L, updateRequest("Maria", null, null, null,
                addressRequest(10L, "999", true), addressRequest(null, "200", false)), false);

        assertThat(kept.getNumber()).isEqualTo("999");
        assertThat(removed.isDeleted()).isTrue();
        assertThat(response.addresses()).extracting(AddressResponse::number).containsExactly("999", "200");
    }

    @Test
    void updatePromotesRemainingAddressWhenTheMainOneIsRemoved() {
        Address main = address(10L, "10", true);
        Address other = address(11L, "11", false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, Role.USER, main, other)));

        UserResponse response = service.update(2L,
                updateRequest("Maria", null, null, null, addressRequest(11L, "11", false)), false);

        assertThat(main.isDeleted()).isTrue();
        assertThat(other.isMainAddress()).isTrue();
        assertThat(response.addresses()).hasSize(1);
        assertThat(response.addresses().get(0).mainAddress()).isTrue();
    }

    @Test
    void updateRejectsAddressFromAnotherUser() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, Role.USER, address(10L, "10", true))));
        UserUpdateRequest request = updateRequest("Maria", null, null, null, addressRequest(999L, "10", true));

        assertThatThrownBy(() -> service.update(2L, request, false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("999");
    }

    @Test
    void updateRejectsMoreThanOneMainAddress() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(
                user(2L, Role.USER, address(10L, "10", true), address(11L, "11", false))));
        UserUpdateRequest request = updateRequest("Maria", null, null, null,
                addressRequest(10L, "10", true), addressRequest(11L, "11", true));

        assertThatThrownBy(() -> service.update(2L, request, false)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updateValidatesTheCepOfEveryAddressSent() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(
                user(2L, Role.USER, address(10L, "10", true))));

        service.update(2L, updateRequest("Maria", null, null, null,
                addressRequest(10L, "10", true), addressRequest(null, "20", false)), false);

        verify(cepValidationService, org.mockito.Mockito.times(2)).validateAndEnrich(any(Address.class));
    }

    // ---------- exclusão lógica ----------

    @Test
    void deleteMarksUserAndAddressesAsDeletedWithoutPhysicalRemoval() {
        Address first = address(10L, "10", true);
        Address second = address(11L, "11", false);
        User user = user(2L, Role.USER, first, second);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        service.delete(2L);

        assertThat(user.isDeleted()).isTrue();
        assertThat(first.isDeleted()).isTrue();
        assertThat(second.isDeleted()).isTrue();
        verify(userRepository, never()).delete(any(User.class));
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteThrowsWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
