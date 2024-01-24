package org.micks.DiscGolfApplication;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class DiscGolfApplication {

	@Autowired
	private DatabaseConfigProperties databaseConfigProperties;

	public static void main(String[] args) {
		SpringApplication.run(DiscGolfApplication.class, args);
	}

	@PostConstruct
	public void init() {
		checkDatabaseConnection();
	}

	private void checkDatabaseConnection() {
		if (databaseConfigProperties.getName() == null) {
			throw new IllegalStateException("Cannot read database configuration");
		} else {
			log.info("Database configuration OK. Using database: {}", databaseConfigProperties.getName());
		}
	}
}
