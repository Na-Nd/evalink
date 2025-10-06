package ru.nand.authservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import ru.nand.authservice.util.ServiceJwtUtil;

@Configuration
public class RestClientConfig {

    private final ServiceJwtUtil serviceJwtUtil;

    @Autowired
    public RestClientConfig(ServiceJwtUtil serviceJwtUtil) {
        this.serviceJwtUtil = serviceJwtUtil;
    }

    @Value("${account-service.url}")
    private String accountServiceUrl;

    /// RestClient для account-service
    @Bean
    public RestClient accountServiceRestClient(RestClient.Builder builder){
        String serviceToken = serviceJwtUtil.generateServiceToken();

        return builder
                .baseUrl(accountServiceUrl)
                .defaultHeader("Authorization", "Bearer " + serviceToken)
                .build();
    }
}
