package br.com.eric.usermanagement.support;

import br.com.eric.usermanagement.domain.entity.Address;
import br.com.eric.usermanagement.domain.entity.User;
import br.com.eric.usermanagement.domain.enums.Role;
import br.com.eric.usermanagement.domain.enums.UserStatus;

public final class TestData {

    private TestData() {
    }

    public static User user(String name, String email, Role role, UserStatus status) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("hash-de-teste");
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    public static Address address(String city, boolean main) {
        Address address = new Address();
        address.setCep("01001000");
        address.setStreet("Rua Teste");
        address.setNumber("10");
        address.setState("SP");
        address.setCity(city);
        address.setDistrict("Centro");
        address.setMainAddress(main);
        return address;
    }
}
