package br.com.eric.usermanagement.mapper;

import br.com.eric.usermanagement.domain.entity.Address;
import br.com.eric.usermanagement.dto.AddressRequest;
import br.com.eric.usermanagement.dto.AddressResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AddressMapper {

    AddressResponse toResponse(Address address);

    @IgnoreEntityManagedFields
    @Mapping(target = "user", ignore = true)
    Address toEntity(AddressRequest request);

    @InheritConfiguration(name = "toEntity")
    void updateEntity(AddressRequest request, @MappingTarget Address address);
}
