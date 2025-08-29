package it.luncent.cloud_storage.user.service;

import it.luncent.cloud_storage.security.model.request.RegistrationRequest;
import it.luncent.cloud_storage.user.entity.User;
import it.luncent.cloud_storage.user.entity.UserRepository;
import it.luncent.cloud_storage.user.mapper.UserMapper;
import it.luncent.cloud_storage.user.model.UserResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@AllArgsConstructor
public class UserService implements UserSericeI, UserDetailsService {
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
    @Transactional
    public UserDetails register(RegistrationRequest registrationRequest) {
        //TODO try catch to handle unique constraint violation, when username exists
        User newUser = userMapper.mapToEntity(registrationRequest);
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        User user = userRepository.save(newUser);
        return loadUserByUsername(user.getUsername());
    }
}
