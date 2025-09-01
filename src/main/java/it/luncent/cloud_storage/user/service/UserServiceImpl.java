package it.luncent.cloud_storage.user.service;

import it.luncent.cloud_storage.security.exception.UsernameExistsException;
import it.luncent.cloud_storage.security.model.request.RegistrationRequest;
import it.luncent.cloud_storage.user.entity.User;
import it.luncent.cloud_storage.user.model.UserResponse;
import it.luncent.cloud_storage.user.repository.UserRepository;
import it.luncent.cloud_storage.user.mapper.UserMapper;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        return new org.springframework.security.core.userdetails.User(
                username,
                user.getPassword(),
                new ArrayList<>()
        );
    }

    @Override
    public UserResponse create(RegistrationRequest registrationRequest) {
        //TODO try catch to handle unique constraint violation, when username exists
        User newUser = userMapper.mapToEntity(registrationRequest);
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        try{
            User user = userRepository.save(newUser);
            return userMapper.mapToResponse(user);
        }catch (DataIntegrityViolationException e){
            throw new UsernameExistsException(newUser.getUsername(), e);
        }
    }
}
