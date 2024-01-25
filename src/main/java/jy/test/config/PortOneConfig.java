package jy.test.config;

import com.siot.IamportRestClient.IamportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PortOneConfig {

    @Value("${key.api}")
    private String apiKey;
    @Value("${key.secret}")
    private String secretKey;

    @Bean
    public IamportClient iamportClient() {
        System.out.println("apiKey = " + apiKey);
        System.out.println("secretKey = " + secretKey);
        return new IamportClient(apiKey, secretKey);
    }
}
