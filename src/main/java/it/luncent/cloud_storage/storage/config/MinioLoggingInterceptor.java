package it.luncent.cloud_storage.storage.config;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class MinioLoggingInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        long startTime = System.currentTimeMillis();
        log.info("→→→ MinIO Request: {} {}", request.method(), request.url());

        try {
            Response response = chain.proceed(request);
            long duration = System.currentTimeMillis() - startTime;

            log.info("←←← MinIO Response: {} {} ({}ms)",
                    response.code(),
                    response.message(),
                    duration);

            return response;
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("✗✗✗ MinIO Request failed after {}ms: {}", duration, e.getMessage());
            throw e;
        }
    }
}
