package com.exelynt.booking;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    protected static final String ADMIN_USER = "admin";
    protected static final String ADMIN_PASS = "Admin@123";
    protected static final String ALICE_USER = "alice";
    protected static final String BOB_USER = "bob";
    protected static final String USER_PASS = "User@123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String adminToken;
    protected String aliceToken;
    protected String bobToken;

    @BeforeEach
    void authenticateSeedUsers() throws Exception {
        adminToken = login(ADMIN_USER, ADMIN_PASS);
        aliceToken = login(ALICE_USER, USER_PASS);
        bobToken = login(BOB_USER, USER_PASS);
    }

    protected String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("token").asText();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String futureTime(int daysAhead, int hour) {
        return LocalDateTime.now()
                .plusDays(daysAhead)
                .withHour(hour).withMinute(0).withSecond(0).withNano(0)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }

    protected String reservationJson(long resourceId, String start, String end, String price) {
        return """
                {"resourceId":%d,"startTime":"%s","endTime":"%s","price":"%s"}"""
                .formatted(resourceId, start, end, price);
    }
}
