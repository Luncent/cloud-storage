package it.luncent.cloud_storage.user.mapper;

import it.luncent.cloud_storage.security.model.request.RegistrationRequest;
import it.luncent.cloud_storage.user.entity.User;
import it.luncent.cloud_storage.user.model.UserResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse mapToResponse(User user);

    @Mapping(target = "id", ignore = true)
    User mapToEntity(RegistrationRequest registrationRequest);
}
