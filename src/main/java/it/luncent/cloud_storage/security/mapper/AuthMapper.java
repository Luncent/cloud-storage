package it.luncent.cloud_storage.security.mapper;

import it.luncent.cloud_storage.security.model.UserModel;
import it.luncent.cloud_storage.security.model.request.UsernamePasswordModel;
import it.luncent.cloud_storage.security.model.response.AuthenticationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    /*default UsernamePasswordAuthenticationToken mapToAuthentication(AuthenticationRequest request){
        return new UsernamePasswordAuthenticationToken(request.username(), request.password());
    }
    default UsernamePasswordAuthenticationToken mapToAuthentication(RegistrationRequest request){
        return new UsernamePasswordAuthenticationToken(request.username(), request.password());
    }*/

    default UsernamePasswordAuthenticationToken mapToAuthentication(UsernamePasswordModel usernamePasswordModel){
        return new UsernamePasswordAuthenticationToken(usernamePasswordModel.getUsername(), usernamePasswordModel.getPassword());
    }

    AuthenticationResponse mapToResponse(UserModel userModel);



}
