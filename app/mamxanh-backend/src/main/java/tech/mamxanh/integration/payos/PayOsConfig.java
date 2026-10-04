package tech.mamxanh.integration.payos;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PayOsProperties.class)
public class PayOsConfig {

    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @ConditionalOnMissingBean(PayOsClient.class)
    public PayOsClient payOsClient(PayOsProperties properties, RestClient.Builder restClientBuilder) {
        return new DefaultPayOsClient(properties, restClientBuilder.build());
    }
}
