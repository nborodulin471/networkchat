package client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "client")
public class ClientConfig {
    private String host;
    private int port;

    @Bean
    public ClientImp client() {
        return new ClientImp(host, port);
    }
}
