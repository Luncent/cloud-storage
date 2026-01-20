package it.luncent.cloud_storage.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;

@Configuration
public class ObjectMapperConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder().deserializers(new StringTrimmingDeserializer());
    }

    private static class StringTrimmingDeserializer extends StringDeserializer {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String str = super.deserialize(p, ctxt);
            return str == null ? null : str.strip();
        }
    }

}
