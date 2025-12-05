package org.micks.DiscGolfApplication.connection

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "db")
data class DatabaseConfigProperties(
    var host: String? = null,
    var name: String? = null,
    var user: String? = null,
    var password: String? = null,
)