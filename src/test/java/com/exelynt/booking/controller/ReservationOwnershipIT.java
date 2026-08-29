package com.exelynt.booking.controller;

import com.exelynt.booking.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationOwnershipIT extends AbstractIntegrationTest {

    private long createReservation(String token, long resourceId, int dayOffset) throws Exception {
        String body = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(resourceId,
                                futureTime(dayOffset, 9), futureTime(dayOffset, 11), "150.00")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    void ownerIsTakenFromTokenNotFromRequestBody() throws Exception {
        String forged = """
                {"resourceId":1,"startTime":"%s","endTime":"%s","price":"150.00",
                 "userId":999,"username":"admin","status":"CONFIRMED"}"""
                .formatted(futureTime(401, 9), futureTime(401, 11));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forged))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void userCanReadOwnReservation() throws Exception {
        long id = createReservation(aliceToken, 1, 402);

        mockMvc.perform(get("/api/reservations/" + id).header("Authorization", bearer(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void userCannotReadAnotherUsersReservation() throws Exception {
        long id = createReservation(aliceToken, 1, 403);

        mockMvc.perform(get("/api/reservations/" + id).header("Authorization", bearer(bobToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminCanReadAnyReservation() throws Exception {
        long id = createReservation(aliceToken, 1, 404);

        mockMvc.perform(get("/api/reservations/" + id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void userListContainsOnlyOwnReservations() throws Exception {
        createReservation(aliceToken, 1, 405);
        createReservation(bobToken, 2, 405);

        String body = mockMvc.perform(get("/api/reservations?size=100")
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode content = objectMapper.readTree(body).get("content");
        assertTrue(content.size() > 0, "alice should have reservations");
        for (JsonNode node : content) {
            assertTrue("alice".equals(node.get("username").asString()),
                    "USER list leaked a reservation owned by " + node.get("username").asString());
        }
    }

    @Test
    void adminListContainsReservationsFromMultipleUsers() throws Exception {
        createReservation(aliceToken, 1, 406);
        createReservation(bobToken, 2, 406);

        String body = mockMvc.perform(get("/api/reservations?size=100")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode content = objectMapper.readTree(body).get("content");
        boolean sawAlice = false;
        boolean sawBob = false;
        for (JsonNode node : content) {
            String owner = node.get("username").asString();
            sawAlice |= "alice".equals(owner);
            sawBob |= "bob".equals(owner);
        }
        assertTrue(sawAlice && sawBob, "ADMIN should see reservations from every user");
    }

    @Test
    void userCannotUpdateAnotherUsersReservation() throws Exception {
        long id = createReservation(aliceToken, 1, 407);

        mockMvc.perform(put("/api/reservations/" + id)
                        .header("Authorization", bearer(bobToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(1, futureTime(408, 9), futureTime(408, 11), "150.00")))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotDeleteAnotherUsersReservation() throws Exception {
        long id = createReservation(aliceToken, 1, 409);

        mockMvc.perform(delete("/api/reservations/" + id).header("Authorization", bearer(bobToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotConfirmOwnReservation() throws Exception {
        long id = createReservation(aliceToken, 1, 410);

        String body = """
                {"resourceId":1,"startTime":"%s","endTime":"%s","price":"150.00","status":"CONFIRMED"}"""
                .formatted(futureTime(411, 9), futureTime(411, 11));

        mockMvc.perform(put("/api/reservations/" + id)
                        .header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void adminCanConfirmAnyReservation() throws Exception {
        long id = createReservation(aliceToken, 1, 412);

        String body = """
                {"resourceId":1,"startTime":"%s","endTime":"%s","price":"150.00","status":"CONFIRMED"}"""
                .formatted(futureTime(413, 9), futureTime(413, 11));

        mockMvc.perform(put("/api/reservations/" + id)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void ownerCanDeleteOwnReservation() throws Exception {
        long id = createReservation(aliceToken, 1, 414);

        mockMvc.perform(delete("/api/reservations/" + id).header("Authorization", bearer(aliceToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/reservations/" + id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }
}
