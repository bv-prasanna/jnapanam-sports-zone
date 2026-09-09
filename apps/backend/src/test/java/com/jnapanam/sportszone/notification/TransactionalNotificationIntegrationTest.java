package com.jnapanam.sportszone.notification;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionalNotificationIntegrationTest {
    private static final Pattern TOKEN = Pattern.compile("\\\"accessToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    @Autowired MockMvc mvc;

    @Test
    void registrationAndPasswordResetCreateCustomerNotifications() throws Exception {
        String email = "notification." + UUID.randomUUID() + "@example.com";
        MvcResult registration = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Notification Customer\",\"email\":\"" + email
                                + "\",\"mobile\":null,\"password\":\"Test@12345\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String token = accessToken(registration);

        mvc.perform(get("/api/v1/notifications/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].category", hasItem("ACCOUNT")))
                .andExpect(jsonPath("$[*].subject", hasItem("Welcome to Jnapanam Sports Zone")));

        mvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/notifications/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].category", hasItem("SECURITY")))
                .andExpect(jsonPath("$[*].subject", hasItem("Password reset requested")));
    }

    private static String accessToken(MvcResult result) throws Exception {
        Matcher matcher = TOKEN.matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) throw new AssertionError("Access token missing");
        return matcher.group(1);
    }
}
