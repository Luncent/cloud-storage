package it.luncent.cloud_storage.config;

import it.luncent.cloud_storage.config.security.JsonUsernamePasswordAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationManager authManager) throws Exception {
        RequestMatcher matcher = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, "/api/auth/sign-in");

        return http
                .csrf(csrfConfigurer -> csrfConfigurer.disable())
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/api/auth/sign-in", "/api/auth/sign-up").permitAll();
                    authorize.anyRequest().authenticated();
                })
                .addFilter(new JsonUsernamePasswordAuthenticationFilter(authManager,matcher))
/*                .logout(logoutConfigurer ->
                        //это спринг будет обрабатывать выход или я могу по этому адресу контролер для выхода сделать?
                        logoutConfigurer.logoutUrl("/api/auth/sing-out")
                                //это что такое
                                .addLogoutHandler(new LogoutHandler() {})
                )*/
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
