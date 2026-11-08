package com.poultry.pms.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Health, dashboard totals, mortality alerts (threshold 0.50 % / critical 1.00 % in the test config), demo data, CORS. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardAlertsApiTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Africa/Nairobi"));

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    private JsonNode json(ResultActions actions) throws Exception {
        return mapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    private long createFlock(int initial) throws Exception {
        String farmBody = "{\"name\":\"Alert Farm " + UUID.randomUUID() + "\"}";
        long farmId = json(mvc.perform(post("/api/farms").contentType(MediaType.APPLICATION_JSON).content(farmBody))
                .andExpect(status().isCreated())).get("id").asLong();
        String flockBody = "{\"flockCode\":\"A-" + UUID.randomUUID().toString().substring(0, 8) + "\",\"farmId\":" + farmId
                + ",\"birdType\":\"Broiler\",\"placementDate\":\"" + TODAY.minusDays(5) + "\",\"initialBirdCount\":" + initial + "}";
        return json(mvc.perform(post("/api/flocks").contentType(MediaType.APPLICATION_JSON).content(flockBody))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private void deaths(long flockId, int count) throws Exception {
        String body = "{\"flockId\":" + flockId + ",\"recordDate\":\"" + TODAY + "\",\"count\":" + count + "}";
        mvc.perform(post("/api/mortality").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
    }

    /** Returns the severity of the OPEN high-mortality alert for the flock, or null when there is none. */
    private String openMortalityAlertSeverity(long flockId) throws Exception {
        JsonNode alerts = json(mvc.perform(get("/api/alerts?status=OPEN")).andExpect(status().isOk()));
        for (JsonNode alert : alerts) {
            if ("HIGH_MORTALITY".equals(alert.get("alertType").asText()) && alert.get("relatedEntityId").asLong() == flockId) {
                return alert.get("severity").asText();
            }
        }
        return null;
    }

    @Test
    void healthEndpointReportsApiAndDatabaseUp() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"));
    }

    @Test
    void mortalityBelowTheThresholdRaisesNoAlert() throws Exception {
        long flockId = createFlock(1000);
        deaths(flockId, 4);   // 0.40 % < 0.50 %
        assertEquals(null, openMortalityAlertSeverity(flockId));
    }

    @Test
    void mortalityAtTheThresholdRaisesAWarning() throws Exception {
        long flockId = createFlock(1000);
        deaths(flockId, 5);   // exactly 0.50 %
        assertEquals("WARNING", openMortalityAlertSeverity(flockId));
    }

    @Test
    void mortalityAtTwiceTheThresholdIsCritical() throws Exception {
        long flockId = createFlock(1000);
        deaths(flockId, 10);   // 1.00 % = threshold x 2
        assertEquals("CRITICAL", openMortalityAlertSeverity(flockId));
    }

    @Test
    void severalSmallRecordsOnTheSameDayAreAddedUpAndRaiseOnlyOneAlert() throws Exception {
        long flockId = createFlock(1000);
        deaths(flockId, 3);
        assertEquals(null, openMortalityAlertSeverity(flockId));
        deaths(flockId, 3);   // 6 that day = 0.60 %
        assertEquals("WARNING", openMortalityAlertSeverity(flockId));
        deaths(flockId, 1);   // still the same single open alert
        JsonNode alerts = json(mvc.perform(get("/api/alerts?status=OPEN")));
        int count = 0;
        for (JsonNode alert : alerts) {
            if ("HIGH_MORTALITY".equals(alert.get("alertType").asText()) && alert.get("relatedEntityId").asLong() == flockId) {
                count++;
            }
        }
        assertEquals(1, count);
    }

    @Test
    void anAlertCanBeResolvedManually() throws Exception {
        long flockId = createFlock(1000);
        deaths(flockId, 6);
        JsonNode alerts = json(mvc.perform(get("/api/alerts?status=OPEN")));
        long alertId = -1;
        for (JsonNode alert : alerts) {
            if (alert.get("relatedEntityId").asLong() == flockId && "HIGH_MORTALITY".equals(alert.get("alertType").asText())) {
                alertId = alert.get("id").asLong();
            }
        }
        assertTrue(alertId > 0, "alert should exist");
        mvc.perform(post("/api/alerts/" + alertId + "/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
        assertEquals(null, openMortalityAlertSeverity(flockId));
    }

    @Test
    void dashboardTotalsAreCalculatedFromStoredRecords() throws Exception {
        JsonNode before = json(mvc.perform(get("/api/dashboard")).andExpect(status().isOk()));
        long flockId = createFlock(500);
        deaths(flockId, 3);
        JsonNode after = json(mvc.perform(get("/api/dashboard")).andExpect(status().isOk()));

        assertEquals(before.get("totalActiveFlocks").asInt() + 1, after.get("totalActiveFlocks").asInt());
        assertEquals(before.get("totalBirds").asInt() + 497, after.get("totalBirds").asInt());
        assertEquals(before.get("mortalityToday").asInt() + 3, after.get("mortalityToday").asInt());
    }

    @Test
    void demoDataLoadsOnlyOnce() throws Exception {
        mvc.perform(post("/api/demo/seed"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seeded").value(true))
                .andExpect(jsonPath("$.flocks").value(3));
        mvc.perform(post("/api/demo/seed")).andExpect(status().isConflict());   // never duplicated
        mvc.perform(get("/api/dashboard")).andExpect(jsonPath("$.demoDataLoaded").value(true));
    }

    @Test
    void demoDataGivesAFeedConversionRatioButAFlockWithoutWeightsGetsNone() throws Exception {
        mvc.perform(post("/api/demo/seed")).andExpect(status().isCreated());
        JsonNode flocks = json(mvc.perform(get("/api/flocks")).andExpect(status().isOk()));
        for (JsonNode flock : flocks) {
            JsonNode perf = json(mvc.perform(get("/api/flocks/" + flock.get("id").asLong() + "/performance")).andExpect(status().isOk()));
            if ("DEMO-001".equals(flock.get("flockCode").asText())) {
                assertEquals("OK", perf.get("fcrStatus").asText());
                assertEquals(1.63, perf.get("fcr").asDouble(), 0.001);
            } else {
                assertEquals("NOT_ENOUGH_DATA", perf.get("fcrStatus").asText());
                assertTrue(perf.get("fcr").isNull());
            }
        }
    }

    @Test
    void unknownUrlsReturnAReadableJsonError() throws Exception {
        mvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void corsAllowsTheViteDevOriginAndRejectsOthers() throws Exception {
        mvc.perform(options("/api/farms")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/farms")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
