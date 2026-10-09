package br.com.eric.usermanagement.controller;

import br.com.eric.usermanagement.domain.enums.Role;
import br.com.eric.usermanagement.domain.enums.UserStatus;
import br.com.eric.usermanagement.dto.*;
import br.com.eric.usermanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Usuários")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Cria um usuário com um ou mais endereços",
            description = "Acesso restrito a ADMIN. Cada CEP é validado no ViaCEP, e cidade e estado são preenchidos a partir dele. "
                    + "Apenas um endereço pode ser o principal (se nenhum for marcado, o primeiro assume).")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            summary = "Busca um usuário por id, com seus endereços",
            description = "ADMIN acessa qualquer usuário. USER acessa apenas o próprio.")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userAuthorization.isSelf(#id, authentication)")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @Operation(
            summary = "Atualiza um usuário e seus endereços",
            description = "ADMIN atualiza qualquer usuário. USER atualiza apenas o próprio e não pode alterar role nem status. "
                    + "Endereços com id são atualizados, sem id são criados e os omitidos são excluídos logicamente. A senha é opcional.")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userAuthorization.isSelf(#id, authentication)")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request,
                               Authentication authentication) {
        boolean admin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return userService.update(id, request, admin);
    }

    @Operation(
            summary = "Exclui um usuário (exclusão lógica)",
            description = "Acesso restrito a ADMIN. O usuário e seus endereços são marcados como excluídos, sem remoção física no banco.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    @Operation(
            summary = "Lista usuários com paginação, filtros e ordenação",
            description = "Acesso restrito a ADMIN. Filtros: search (nome ou e-mail), city (cidade do endereço principal), status e role. "
                    + "Ordenação por name, email, status, role, createdAt ou city, por exemplo sort=city,desc.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserSummaryResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return userService.search(new UserFilter(search, city, status, role), pageable);
    }
}
