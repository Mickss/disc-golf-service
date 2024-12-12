package org.micks.DiscGolfApplication.connection;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "db")
@Getter
@Setter
public class DatabaseConfigProperties {

    private String name;
    private String user;
    private String password;
}