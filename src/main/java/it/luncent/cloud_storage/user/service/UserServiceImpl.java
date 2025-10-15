package it.luncent.cloud_storage.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.luncent.cloud_storage.security.exception.UsernameExistsException;
import it.luncent.cloud_storage.security.model.UserModel;
import it.luncent.cloud_storage.security.model.request.RegistrationRequest;
import it.luncent.cloud_storage.user.entity.User;
import it.luncent.cloud_storage.user.mapper.UserMapper;
import it.luncent.cloud_storage.user.model.UserResponse;
import it.luncent.cloud_storage.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

import static java.lang.String.format;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {
    private static final String USER_ALREADY_EXISTS_TEMPLATE = "User with username %s already exists";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = findByUsername(username);
        return new org.springframework.security.core.userdetails.User(
                createPrincipal(user),
                user.getPassword(),
                new ArrayList<>()
        );
    }

    @Override
    public UserResponse create(RegistrationRequest registrationRequest) {
        User newUser = userMapper.mapToEntity(registrationRequest);
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        try{
            User user = userRepository.save(newUser);
            return userMapper.mapToResponse(user);
        }catch (DataIntegrityViolationException e){
            throw new UsernameExistsException(format(USER_ALREADY_EXISTS_TEMPLATE, newUser.getUsername()), e);
        }
    }

    private String createPrincipal(User user) {
        UserModel userModel = new UserModel(user.getId(), user.getUsername());
        try {
           return objectMapper.writeValueAsString(userModel);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Bad credentials"));
    }
}
