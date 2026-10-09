package br.com.eric.usermanagement.service;

import br.com.eric.usermanagement.domain.entity.Address;
import br.com.eric.usermanagement.domain.entity.User;
import br.com.eric.usermanagement.domain.enums.UserStatus;
import br.com.eric.usermanagement.dto.*;
import br.com.eric.usermanagement.exception.BusinessRuleException;
import br.com.eric.usermanagement.exception.EmailAlreadyExistsException;
import br.com.eric.usermanagement.exception.ResourceNotFoundException;
import br.com.eric.usermanagement.mapper.AddressMapper;
import br.com.eric.usermanagement.mapper.UserMapper;
import br.com.eric.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final PasswordEncoder passwordEncoder;
    private final CepValidationService cepValidationService;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIncludingDeleted(email)) {
            throw new EmailAlreadyExistsException();
        }

        User user = userMapper.toEntity(request);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);

        List<Address> addresses = request.addresses().stream().map(addressMapper::toEntity).toList();
        addresses.forEach(cepValidationService::validateAndEnrich);
        ensureSingleMainAddress(addresses);
        addresses.forEach(user::addAddress);

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userMapper.toResponse(findActive(id));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request, boolean admin) {
        User user = findActive(id);

        if (!admin) {
            boolean changesRole = request.role() != null && request.role() != user.getRole();
            boolean changesStatus = request.status() != null && request.status() != user.getStatus();
            if (changesRole || changesStatus) {
                throw new AccessDeniedException("Apenas ADMIN pode alterar perfil ou status");
            }
        } else {
            if (request.role() != null) user.setRole(request.role());
            if (request.status() != null) user.setStatus(request.status());
        }

        user.setName(request.name());
        user.setPhone(request.phone());
        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        syncAddresses(user, request.addresses());

        userRepository.flush();
        return userMapper.toResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = findActive(id);
        user.setDeleted(true);
        user.getAddresses().forEach(address -> address.setDeleted(true));
    }

    private User findActive(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));
    }

    private void syncAddresses(User user, List<AddressRequest> requests) {
        Map<Long, Address> current = user.getAddresses().stream()
                .collect(Collectors.toMap(Address::getId, Function.identity()));
        Set<Long> kept = new HashSet<>();

        for (AddressRequest request : requests) {
            if (request.id() == null) {
                Address created = addressMapper.toEntity(request);
                cepValidationService.validateAndEnrich(created);
                user.addAddress(created);
                continue;
            }
            Address existing = current.get(request.id());
            if (existing == null) {
                throw new BusinessRuleException("Endereço " + request.id() + " não pertence ao usuário");
            }
            addressMapper.updateEntity(request, existing);
            cepValidationService.validateAndEnrich(existing);
            kept.add(request.id());
        }

        current.values().stream()
                .filter(address -> !kept.contains(address.getId()))
                .forEach(address -> address.setDeleted(true));

        ensureSingleMainAddress(user.getAddresses().stream().filter(a -> !a.isDeleted()).toList());
    }

    private void ensureSingleMainAddress(List<Address> addresses) {
        long mains = addresses.stream().filter(Address::isMainAddress).count();
        if (mains > 1) {
            throw new BusinessRuleException("Apenas um endereço pode ser o principal");
        }
        if (mains == 0) {
            addresses.get(0).setMainAddress(true);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> search(UserFilter filter, Pageable pageable) {
        return PageResponse.from(userRepository.search(filter, pageable));
    }
}
