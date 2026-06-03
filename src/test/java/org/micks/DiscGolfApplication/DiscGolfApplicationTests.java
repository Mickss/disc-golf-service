package org.micks.DiscGolfApplication;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.micks.DiscGolfApplication.events.EventReminderScheduler;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"spring.flyway.enabled=false",
		"db.name=mock-db",
		"db.host=localhost",
		"email.service.url=http://localhost",
})
@Slf4j
class DiscGolfApplicationTests {

	@MockitoBean
	private EventReminderScheduler eventReminderScheduler;

	@Test
	void contextLoads() {
		log.info("Test initialized properly");
	}

}
