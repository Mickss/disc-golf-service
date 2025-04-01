package org.micks.DiscGolfApplication

import jakarta.annotation.PostConstruct
import org.micks.DiscGolfApplication.connection.DatabaseConfigProperties
import org.micks.DiscGolfApplication.util.logger
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DiscGolfApplication(private val databaseConfigProperties: DatabaseConfigProperties) {
    private val log = logger()

    @PostConstruct
    fun init() {
        checkDatabaseConnection()
    }

    fun checkDatabaseConnection() {
        if (databaseConfigProperties.name == null) {
            throw IllegalStateException("Cannot read database configuration")
        }
        log.info("Database configuration OK. Using database: {}", databaseConfigProperties.name)
    }
}

fun main(args: Array<String>) {
    runApplication<DiscGolfApplication>(*args)
}