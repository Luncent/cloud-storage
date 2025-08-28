package it.luncent.cloud_storage.config.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.BufferedReader;
import java.util.stream.Collectors;

@Slf4j
public class JsonUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String USERNAME_ATTRIBUTE_NAME = "username";
    private static final String PASSWORD_ATTRIBUTE_NAME = "password";

    public JsonUsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager, RequestMatcher matcher) {
        super(authenticationManager);
        setRequiresAuthenticationRequestMatcher(matcher);
    }

    @Override
    @Nullable
    protected String obtainUsername(HttpServletRequest request) {
        if(request.getAttribute(USERNAME_ATTRIBUTE_NAME) == null) {
            extractAuthNData(request);
        }

        return (String)request.getAttribute(USERNAME_ATTRIBUTE_NAME);
    }

    @Override
    @Nullable
    protected String obtainPassword(HttpServletRequest request) {
        if(request.getAttribute(USERNAME_ATTRIBUTE_NAME) == null) {
            extractAuthNData(request);
        }

        return (String)request.getAttribute(PASSWORD_ATTRIBUTE_NAME);
    }

    private void extractAuthNData(HttpServletRequest request) {
        try(BufferedReader bufferedReader = request.getReader()) {
            String json = bufferedReader.lines().collect(Collectors.joining());
            JsonNode node = objectMapper.readTree(json);
            request.setAttribute(USERNAME_ATTRIBUTE_NAME, node.get(USERNAME_ATTRIBUTE_NAME).asText());
            request.setAttribute(PASSWORD_ATTRIBUTE_NAME, node.get(PASSWORD_ATTRIBUTE_NAME).asText());
        }catch (Exception ex){
            log.error(ex.getMessage(), ex);
            throw new AuthenticationServiceException(ex.getMessage(), ex);
        }
    }
}
