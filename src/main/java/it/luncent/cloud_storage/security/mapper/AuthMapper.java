package it.luncent.cloud_storage.security.mapper;

import it.luncent.cloud_storage.security.model.User;
import it.luncent.cloud_storage.security.model.request.UsernamePasswordModel;
import it.luncent.cloud_storage.security.model.response.AuthenticationResponse;
import org.mapstruct.Mapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    default UsernamePasswordAuthenticationToken mapToAuthentication(UsernamePasswordModel usernamePasswordModel){
        return new UsernamePasswordAuthenticationToken(usernamePasswordModel.getUsername(), usernamePasswordModel.getPassword());
    }

    AuthenticationResponse mapToResponse(User user);
}
