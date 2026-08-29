package com.exelynt.booking.controller;

import com.exelynt.booking.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResourceControllerIT extends AbstractIntegrationTest {

    private static final String VALID_BODY =
            "{\"name\":\"Test Room\",\"type\":\"ROOM\",\"description\":\"d\",\"pricePerUnit\":\"99.99\"}";

    private long createResource(String name) throws Exception {
        String body = mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"type\":\"ROOM\",\"pricePerUnit\":\"10.00\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    void userCanListResources() throws Exception {
        mockMvc.perform(get("/api/resources").header("Authorization", bearer(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void userCanReadSingleResource() throws Exception {
        mockMvc.perform(get("/api/resources/1").header("Authorization", bearer(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void userCannotCreateResource() throws Exception {
        mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void userCannotUpdateResource() throws Exception {
        mockMvc.perform(put("/api/resources/1")
                        .header("Authorization", bearer(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotDeleteResource() throws Exception {
        mockMvc.perform(delete("/api/resources/1").header("Authorization", bearer(aliceToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateResourceAndReceivesLocationHeader() throws Exception {
        mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Test Room"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void adminCanUpdateResource() throws Exception {
        long id = createResource("Updatable Room");

        mockMvc.perform(put("/api/resources/" + id)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\",\"type\":\"ROOM\",\"available\":false,\"pricePerUnit\":\"55.50\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.pricePerUnit").value(55.50));
    }

    @Test
    void adminCanDeleteResourceWithoutReservations() throws Exception {
        long id = createResource("Disposable Room");

        mockMvc.perform(delete("/api/resources/" + id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/resources/" + id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownResourceReturns404() throws Exception {
        mockMvc.perform(get("/api/resources/999999").header("Authorization", bearer(aliceToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidResourceBodyReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"type\":\"\",\"pricePerUnit\":\"-5.00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.type").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.pricePerUnit").isNotEmpty());
    }
}
