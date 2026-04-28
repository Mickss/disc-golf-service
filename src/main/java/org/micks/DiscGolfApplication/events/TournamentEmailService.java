package org.micks.DiscGolfApplication.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class TournamentEmailService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${email.service.url}")
    private String emailServiceUrl;

    public boolean sendRawEmail(String userEmail, String subject, String htmlBody) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("toEmail", userEmail);
        payload.put("subject", subject);
        payload.put("body", htmlBody);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForEntity(emailServiceUrl + "/email/send-raw", request, Void.class);
            log.info("The RAW email was successfully sent to: {}", userEmail);
            return true;
        } catch (Exception e) {
            log.error("Error sending a RAW email to: {}", userEmail, e);
            return false;
        }
    }
}
