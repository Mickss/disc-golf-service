package org.micks.DiscGolfApplication;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.flyway.enabled=false",
		"db.name=mock-db",
})
@Slf4j
class DiscGolfApplicationTests {

	@Test
	void contextLoads() {
		log.info("Test initialized properly");
	}

}
