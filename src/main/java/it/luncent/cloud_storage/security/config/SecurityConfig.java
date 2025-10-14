package it.luncent.cloud_storage.security.config;

import it.luncent.cloud_storage.security.exception.customized_handler.CustomAccessDeniedHandler;
import it.luncent.cloud_storage.security.exception.customized_handler.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

//TODO попробовать добавить csrf токен
// настроить корс под фронт
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationManager authManager) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                //.cors(cors-> cors.configurationSource())
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/api/auth/sign-out").authenticated();
                    authorize.requestMatchers(
                            "api/auth/**",
                            "/free"
                    ).permitAll();
                    authorize.anyRequest().authenticated();
                })
                .exceptionHandling(exceptionHandlerConfigurer -> {
                    exceptionHandlerConfigurer.accessDeniedHandler(accessDeniedHandler);
                    exceptionHandlerConfigurer.authenticationEntryPoint(authenticationEntryPoint);
                })
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
