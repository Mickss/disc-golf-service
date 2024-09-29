package org.micks.DiscGolfApplication;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;

@Service
@Slf4j
public class UserService {

    private final OkHttpClient httpClient = new OkHttpClient();


    public boolean isUserAdmin(String token) {
        return checkUserRole(token);
    }

    private boolean checkUserRole(String token) {
        String authServiceUrl = "http://localhost:25003/users/logged-in";

        log.info("Sending request to auth service with token: " + token);

        Request request = new Request.Builder()
                .url(authServiceUrl)
                .addHeader("Authorization", token)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Error response from auth service: " + response.code() + " " + response.message());
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = Objects.requireNonNull(response.body()).string();
            log.info("Auth service response: " + responseBody);

            ObjectMapper objectMapper = new ObjectMapper();
            UserDTO userDTO = objectMapper.readValue(responseBody, UserDTO.class);

            return "ADMIN".equals(userDTO.getRole());
        } catch (IOException e) {
            throw new IllegalStateException("Error while getting user response", e);
        }
    }
}