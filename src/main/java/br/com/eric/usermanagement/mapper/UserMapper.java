package br.com.eric.usermanagement.mapper;

import br.com.eric.usermanagement.domain.entity.User;
import br.com.eric.usermanagement.dto.UserCreateRequest;
import br.com.eric.usermanagement.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = AddressMapper.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {

    @Mapping(target = "addresses", source = "activeAddresses")
    UserResponse toResponse(User user);

    @IgnoreEntityManagedFields
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "activeAddresses", ignore = true)
    User toEntity(UserCreateRequest request);
}
