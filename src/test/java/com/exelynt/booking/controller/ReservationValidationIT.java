package com.exelynt.booking.controller;

import com.exelynt.booking.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationValidationIT extends AbstractIntegrationTest {

    private static final long UNAVAILABLE_RESOURCE_ID = 5;

    private void expectCreateStatus(String token, String body, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void pastStartTimeIsRejected() throws Exception {
        String past = LocalDateTime.now().minusDays(3)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(1, past, futureTime(601, 11), "50.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.startTime").isNotEmpty());
    }

    @Test
    void endTimeBeforeStartTimeIsRejected() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(1, futureTime(602, 15), futureTime(602, 9), "50.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.endAfterStart").isNotEmpty());
    }

    @Test
    void negativePriceIsRejected() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(1, futureTime(603, 9), futureTime(603, 11), "-10.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").isNotEmpty());
    }

    @Test
    void missingRequiredFieldsAreReported() throws Exception {
        expectCreateStatus(aliceToken, "{}", 400);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.fieldErrors.resourceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.startTime").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.endTime").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.price").isNotEmpty());
    }

    @Test
    void malformedJsonIsRejected() throws Exception {
        expectCreateStatus(aliceToken, "{not json", 400);
    }

    @Test
    void overlappingReservationIsRejected() throws Exception {
        String body = reservationJson(3, futureTime(610, 9), futureTime(610, 11), "220.50");

        expectCreateStatus(aliceToken, body, 201);
        expectCreateStatus(bobToken, body, 409);
    }

    @Test
    void unavailableResourceIsRejected() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(UNAVAILABLE_RESOURCE_ID,
                                futureTime(611, 9), futureTime(611, 11), "300.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void unknownResourceReturns404() throws Exception {
        expectCreateStatus(aliceToken,
                reservationJson(999999, futureTime(612, 9), futureTime(612, 11), "10.00"), 404);
    }

    @Test
    void unknownSortFieldReturns400() throws Exception {
        mockMvc.perform(get("/api/reservations?sort=maliciousField,desc")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void invalidStatusFilterReturns400() throws Exception {
        mockMvc.perform(get("/api/reservations?status=NOT_A_STATUS")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paginationReturnsMetadataAndRespectsSize() throws Exception {
        expectCreateStatus(aliceToken,
                reservationJson(4, futureTime(620, 9), futureTime(620, 11), "45.00"), 201);
        expectCreateStatus(aliceToken,
                reservationJson(4, futureTime(621, 9), futureTime(621, 11), "45.00"), 201);

        String body = mockMvc.perform(get("/api/reservations?page=0&size=1")
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andReturn().getResponse().getContentAsString();

        assertTrue(objectMapper.readTree(body).get("content").size() == 1);
    }

    @Test
    void filteringByStatusReturnsOnlyThatStatus() throws Exception {
        String body = mockMvc.perform(get("/api/reservations?status=PENDING&size=100")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        for (JsonNode node : objectMapper.readTree(body).get("content")) {
            assertTrue("PENDING".equals(node.get("status").asString()));
        }
    }

    @Test
    void filteringByPriceRangeIsApplied() throws Exception {
        String body = mockMvc.perform(get("/api/reservations?minPrice=100&size=100")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        for (JsonNode node : objectMapper.readTree(body).get("content")) {
            assertTrue(node.get("price").decimalValue().doubleValue() >= 100);
        }
    }
}
