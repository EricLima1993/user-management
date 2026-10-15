package br.com.eric.usermanagement.config;

import br.com.eric.usermanagement.domain.entity.Address;
import br.com.eric.usermanagement.domain.entity.User;
import br.com.eric.usermanagement.domain.enums.Role;
import br.com.eric.usermanagement.domain.enums.UserStatus;
import br.com.eric.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Popula o banco H2 do perfil dev com usuários de exemplo, para não recriá-los
 * a cada reinício. Não existe nos perfis test e prod.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    static final String SAMPLE_PASSWORD = "Senha@123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String hash = passwordEncoder.encode(SAMPLE_PASSWORD); // um hash só, reaproveitado
        int created = 0;

        for (User user : samples(hash)) {
            if (userRepository.existsByEmailIncludingDeleted(user.getEmail())) {
                continue; // idempotente, como o AdminSeeder
            }
            userRepository.save(user);
            created++;
        }
        log.info("Dados de exemplo (dev): {} usuários criados. Senha de todos: {}", created, SAMPLE_PASSWORD);
    }

    private List<User> samples(String hash) {
        return List.of(
                user("Ana Souza", "ana.souza@teste.com", "11999990001", Role.ADMIN, UserStatus.ACTIVE, hash,
                        address("01001000", "Praça da Sé", "100", "Sé", "São Paulo", "SP", true)),
                user("Bruno Lima", "bruno.lima@teste.com", "11999990002", Role.USER, UserStatus.ACTIVE, hash,
                        address("01310100", "Avenida Paulista", "1000", "Bela Vista", "São Paulo", "SP", true)),
                user("Carla Mendes", "carla.mendes@teste.com", "21999990003", Role.USER, UserStatus.ACTIVE, hash,
                        address("20040020", "Rua da Assembleia", "10", "Centro", "Rio de Janeiro", "RJ", true),
                        address("01001000", "Praça da Sé", "200", "Sé", "São Paulo", "SP", false)),
                user("Diego Rocha", "diego.rocha@teste.com", "31999990004", Role.USER, UserStatus.INACTIVE, hash,
                        address("30130010", "Avenida Afonso Pena", "500", "Centro", "Belo Horizonte", "MG", true)),
                user("Elisa Barros", "elisa.barros@teste.com", "71999990005", Role.USER, UserStatus.ACTIVE, hash,
                        address("40026010", "Rua Chile", "30", "Centro", "Salvador", "BA", true)),
                user("Felipe Costa", "felipe.costa@teste.com", "41999990006", Role.USER, UserStatus.ACTIVE, hash,
                        address("80010000", "Rua XV de Novembro", "70", "Centro", "Curitiba", "PR", true)),
                user("Gabriela Nunes", "gabriela.nunes@teste.com", "51999990007", Role.USER, UserStatus.ACTIVE, hash,
                        address("90010000", "Rua dos Andradas", "90", "Centro Histórico", "Porto Alegre", "RS", true)),
                user("Henrique Alves", "henrique.alves@teste.com", "61999990008", Role.ADMIN, UserStatus.ACTIVE, hash,
                        address("70040010", "Setor de Autarquias", "15", "Zona Cívico-Administrativa", "Brasília", "DF", true)),
                user("Isabela Freitas", "isabela.freitas@teste.com", "85999990009", Role.USER, UserStatus.ACTIVE, hash,
                        address("60060440", "Rua Major Facundo", "60", "Centro", "Fortaleza", "CE", true)),
                user("João Pereira", "joao.pereira@teste.com", "92999990010", Role.USER, UserStatus.INACTIVE, hash,
                        address("69005010", "Avenida Eduardo Ribeiro", "80", "Centro", "Manaus", "AM", true)),
                user("Karina Duarte", "karina.duarte@teste.com", "81999990011", Role.USER, UserStatus.ACTIVE, hash,
                        address("50010000", "Rua do Imperador", "40", "Santo Antônio", "Recife", "PE", true)),
                user("Lucas Martins", "lucas.martins@teste.com", "62999990012", Role.USER, UserStatus.ACTIVE, hash,
                        address("74003010", "Rua 3", "25", "Setor Central", "Goiânia", "GO", true))
        );
    }

    private User user(String name, String email, String phone, Role role, UserStatus status,
                      String passwordHash, Address... addresses) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordHash);
        user.setRole(role);
        user.setStatus(status);
        for (Address address : addresses) {
            user.addAddress(address);
        }
        return user;
    }

    private Address address(String cep, String street, String number, String district,
                            String city, String state, boolean main) {
        Address address = new Address();
        address.setCep(cep);
        address.setStreet(street);
        address.setNumber(number);
        address.setDistrict(district);
        address.setCity(city);
        address.setState(state);
        address.setMainAddress(main);
        return address;
    }
}