package it.luncent.cloud_storage.resource.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.luncent.cloud_storage.common.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.impl.FileCountLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class ServletExceptionHandler extends GenericFilterBean {

    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (FileCountLimitExceededException e) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            String message = "files count limit exceeded";
            ErrorResponse errorResponse = new ErrorResponse(message);
            String parsedResponse = objectMapper.writeValueAsString(errorResponse);
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            httpResponse.getWriter().write(parsedResponse);
        }
    }
}
