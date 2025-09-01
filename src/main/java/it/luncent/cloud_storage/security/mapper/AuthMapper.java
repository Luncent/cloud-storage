package it.luncent.cloud_storage.security.mapper;

import it.luncent.cloud_storage.security.model.request.AuthenticationRequest;
import it.luncent.cloud_storage.security.model.request.RegistrationRequest;
import it.luncent.cloud_storage.security.model.response.AuthenticationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    default UsernamePasswordAuthenticationToken mapToAuthentication(AuthenticationRequest request){
        return new UsernamePasswordAuthenticationToken(request.username(), request.password());
    }

    @Mapping(target = "username", expression = "java(authentication.getName())")
    AuthenticationResponse mapToResponse(Authentication authentication);

    default UsernamePasswordAuthenticationToken mapToAuthentication(RegistrationRequest request){
        return new UsernamePasswordAuthenticationToken(request.username(), request.password());
    }

}
