package server;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "server")
public class ServerConfig {
    private String host;
    private int port;

    @Bean
    public Server server() {
        return new Server(host, port);
    }
}
