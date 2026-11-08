package com.poultry.pms.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Farm, flock and mortality rules, tested through the real HTTP layer (MockMvc) and an in-memory SQLite database.
 * Every test runs in a transaction that is rolled back afterwards, so tests never affect each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FlockMortalityApiTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Africa/Nairobi"));

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    // ------------------------------------------------------------ helpers

    private JsonNode body(String json, org.springframework.test.web.servlet.ResultActions actions) throws Exception {
        return mapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    private long createFarm() throws Exception {
        String json = "{\"name\":\"Test Farm " + UUID.randomUUID() + "\"}";
        var result = mvc.perform(post("/api/farms").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        return body(json, result).get("id").asLong();
    }

    private String flockJson(long farmId, String code, int initial) {
        return "{\"flockCode\":\"" + code + "\",\"farmId\":" + farmId + ",\"birdType\":\"Broiler\","
                + "\"placementDate\":\"" + TODAY.minusDays(10) + "\",\"initialBirdCount\":" + initial + "}";
    }

    private long createFlock(long farmId, int initial) throws Exception {
        String code = "T-" + UUID.randomUUID().toString().substring(0, 8);
        var result = mvc.perform(post("/api/flocks").contentType(MediaType.APPLICATION_JSON).content(flockJson(farmId, code, initial)))
                .andExpect(status().isCreated());
        return body("", result).get("id").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions recordDeaths(long flockId, LocalDate date, int count, String opId) throws Exception {
        String op = opId == null ? "" : ",\"clientOperationId\":\"" + opId + "\"";
        String json = "{\"flockId\":" + flockId + ",\"recordDate\":\"" + date + "\",\"count\":" + count + op + "}";
        return mvc.perform(post("/api/mortality").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // ------------------------------------------------------------ tests

    @Test
    void flockWithNegativeInitialCountIsRejected() throws Exception {
        long farmId = createFarm();
        mvc.perform(post("/api/flocks").contentType(MediaType.APPLICATION_JSON).content(flockJson(farmId, "NEG-1", -5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.initialBirdCount").exists());
    }

    @Test
    void duplicateFlockCodeIsRejectedCaseInsensitively() throws Exception {
        long farmId = createFarm();
        String code = "DUP-" + UUID.randomUUID().toString().substring(0, 6);
        mvc.perform(post("/api/flocks").contentType(MediaType.APPLICATION_JSON).content(flockJson(farmId, code, 100)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/flocks").contentType(MediaType.APPLICATION_JSON).content(flockJson(farmId, code.toLowerCase(), 100)))
                .andExpect(status().isConflict());
    }

    @Test
    void recordedDeathsReduceTheDerivedCurrentBirdCount() throws Exception {
        long flockId = createFlock(createFarm(), 100);
        recordDeaths(flockId, TODAY, 5, null).andExpect(status().isCreated());
        mvc.perform(get("/api/flocks/" + flockId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialBirdCount").value(100))
                .andExpect(jsonPath("$.totalMortality").value(5))
                .andExpect(jsonPath("$.currentBirdCount").value(95));
    }

    @Test
    void mortalityLargerThanTheBirdsAliveIsRejected() throws Exception {
        long flockId = createFlock(createFarm(), 10);
        recordDeaths(flockId, TODAY, 11, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.count").exists());
        mvc.perform(get("/api/flocks/" + flockId)).andExpect(jsonPath("$.currentBirdCount").value(10));
    }

    @Test
    void zeroMortalityIsRejected() throws Exception {
        long flockId = createFlock(createFarm(), 10);
        recordDeaths(flockId, TODAY, 0, null).andExpect(status().isBadRequest());
    }

    @Test
    void mortalityInTheFutureIsRejected() throws Exception {
        long flockId = createFlock(createFarm(), 10);
        recordDeaths(flockId, TODAY.plusDays(1), 1, null).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void retryingTheSameClientOperationIdDoesNotDoubleCount() throws Exception {
        long flockId = createFlock(createFarm(), 100);
        String op = UUID.randomUUID().toString();
        recordDeaths(flockId, TODAY, 5, op).andExpect(status().isCreated());
        recordDeaths(flockId, TODAY, 5, op).andExpect(status().isOk());   // 200 = already processed
        mvc.perform(get("/api/flocks/" + flockId)).andExpect(jsonPath("$.totalMortality").value(5));
        mvc.perform(get("/api/flocks/" + flockId + "/mortality")).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void flockCannotBeEditedBelowTheDeathsAlreadyRecorded() throws Exception {
        long farmId = createFarm();
        long flockId = createFlock(farmId, 100);
        recordDeaths(flockId, TODAY, 5, null).andExpect(status().isCreated());
        mvc.perform(put("/api/flocks/" + flockId).contentType(MediaType.APPLICATION_JSON)
                        .content(flockJson(farmId, "EDIT-" + UUID.randomUUID().toString().substring(0, 6), 3)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.initialBirdCount").exists());
    }

    @Test
    void unknownFlockReturns404() throws Exception {
        mvc.perform(get("/api/flocks/999999")).andExpect(status().isNotFound());
    }

    @Test
    void sameDayDeathsAreTotalledCorrectly() throws Exception {
        long flockId = createFlock(createFarm(), 1000);
        recordDeaths(flockId, TODAY, 2, null).andExpect(status().isCreated());
        recordDeaths(flockId, TODAY, 3, null).andExpect(status().isCreated());
        var result = mvc.perform(get("/api/flocks/" + flockId + "/mortality")).andExpect(status().isOk());
        JsonNode list = body("", result);
        assertEquals(2, list.size());
        assertEquals(5, list.get(0).get("count").asInt() + list.get(1).get("count").asInt());
    }
}
