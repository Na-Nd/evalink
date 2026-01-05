package ru.nand.postservice.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/// Конвертеры для сериализации / десериализации списка из стринги в JSON
@Configuration
public class R2dbcConfig {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions(ObjectMapper objectMapper) {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new JsonbToListConverter(objectMapper));
        converters.add(new ListToJsonbConverter(objectMapper));

        return new R2dbcCustomConversions(R2dbcCustomConversions.StoreConversions.NONE, converters);
    }

    @ReadingConverter
    static class JsonbToListConverter implements Converter<String, List<String>> {
        private final ObjectMapper mapper;
        public JsonbToListConverter(ObjectMapper mapper) { this.mapper = mapper; }
        @Override
        public List<String> convert(String source) {
            try {
                if (source == null) return Collections.emptyList();
                return mapper.readValue(source, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
    }

    @WritingConverter
    static class ListToJsonbConverter implements Converter<List<String>, String> {
        private final ObjectMapper mapper;
        public ListToJsonbConverter(ObjectMapper mapper) { this.mapper = mapper; }
        @Override
        public String convert(List<String> source) {
            try {
                return mapper.writeValueAsString(source);
            } catch (Exception e) {
                return "[]";
            }
        }
    }
}
