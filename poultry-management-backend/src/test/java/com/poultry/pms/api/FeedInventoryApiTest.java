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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Feed stock rules: stock-in / usage are separate operations, stock never goes below zero, history is kept. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FeedInventoryApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    private JsonNode json(ResultActions actions) throws Exception {
        return mapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    private String uniqueName() {
        return "Feed " + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Creates a feed item with the given opening stock and reorder threshold; returns its id. */
    private long createItem(String name, double opening, double threshold) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"feedType\":\"Grower\",\"reorderThreshold\":" + threshold
                + ",\"openingQuantity\":" + opening + "}";
        ResultActions result = mvc.perform(post("/api/feed-items").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        return json(result).get("id").asLong();
    }

    private ResultActions use(long itemId, double qty, String opId) throws Exception {
        String op = opId == null ? "" : ",\"clientOperationId\":\"" + opId + "\"";
        return mvc.perform(post("/api/feed-items/" + itemId + "/usage").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":" + qty + op + "}"));
    }

    private ResultActions stockIn(long itemId, double qty) throws Exception {
        return mvc.perform(post("/api/feed-items/" + itemId + "/stock-in").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":" + qty + "}"));
    }

    private double quantityOnHand(long itemId) throws Exception {
        return json(mvc.perform(get("/api/feed-items/" + itemId)).andExpect(status().isOk())).get("quantityOnHand").asDouble();
    }

    @Test
    void openingStockUsageAndStockInChangeTheQuantityAndLeaveAHistory() throws Exception {
        long id = createItem(uniqueName(), 100, 10);
        assertEquals(100.0, quantityOnHand(id), 0.0001);

        use(id, 30, null).andExpect(status().isCreated());
        assertEquals(70.0, quantityOnHand(id), 0.0001);

        stockIn(id, 50).andExpect(status().isCreated());
        assertEquals(120.0, quantityOnHand(id), 0.0001);

        // opening stock + usage + stock-in = 3 movements, newest first, each with the balance after it
        mvc.perform(get("/api/feed-items/" + id + "/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].movementType").value("STOCK_IN"))
                .andExpect(jsonPath("$[0].quantityAfter").value(120.0));
    }

    @Test
    void usageLargerThanTheStockOnHandIsRejectedAndStockIsUnchanged() throws Exception {
        long id = createItem(uniqueName(), 40, 5);
        use(id, 41, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
        assertEquals(40.0, quantityOnHand(id), 0.0001);
    }

    @Test
    void usingExactlyAllTheStockIsAllowedAndLeavesZero() throws Exception {
        long id = createItem(uniqueName(), 25, 0);
        use(id, 25, null).andExpect(status().isCreated());
        assertEquals(0.0, quantityOnHand(id), 0.0001);
    }

    @Test
    void zeroNegativeOrMissingQuantitiesAreRejected() throws Exception {
        long id = createItem(uniqueName(), 10, 1);
        use(id, 0, null).andExpect(status().isBadRequest());
        use(id, -3, null).andExpect(status().isBadRequest());
        mvc.perform(post("/api/feed-items/" + id + "/stock-in").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        assertEquals(10.0, quantityOnHand(id), 0.0001);
    }

    @Test
    void retryingTheSameClientOperationIdDoesNotUseTheFeedTwice() throws Exception {
        long id = createItem(uniqueName(), 100, 10);
        String op = UUID.randomUUID().toString();
        use(id, 30, op).andExpect(status().isCreated());
        use(id, 30, op).andExpect(status().isOk());   // already processed
        assertEquals(70.0, quantityOnHand(id), 0.0001);
        mvc.perform(get("/api/feed-items/" + id + "/movements")).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void lowStockAlertOpensAtTheThresholdAndResolvesWhenStockIsReplenished() throws Exception {
        String name = uniqueName();
        long id = createItem(name, 100, 50);

        // 100 -> 50 kg: exactly AT the threshold counts as low
        use(id, 50, null).andExpect(status().isCreated());
        JsonNode open = json(mvc.perform(get("/api/alerts?status=OPEN")).andExpect(status().isOk()));
        boolean found = false;
        for (JsonNode alert : open) {
            if ("LOW_STOCK".equals(alert.get("alertType").asText()) && alert.get("message").asText().contains(name)) {
                found = true;
                assertEquals("PROTOTYPE_GENERATED", alert.get("source").asText());
            }
        }
        assertTrue(found, "expected an OPEN low-stock alert for " + name);

        // restock above the threshold -> the alert resolves itself
        stockIn(id, 100).andExpect(status().isCreated());
        JsonNode stillOpen = json(mvc.perform(get("/api/alerts?status=OPEN")).andExpect(status().isOk()));
        for (JsonNode alert : stillOpen) {
            assertTrue(!alert.get("message").asText().contains(name), "alert for " + name + " should be resolved");
        }
    }

    @Test
    void duplicateFeedNamesAreRejected() throws Exception {
        String name = uniqueName();
        createItem(name, 1, 1);
        String body = "{\"name\":\"" + name.toUpperCase() + "\",\"feedType\":\"x\",\"reorderThreshold\":1}";
        mvc.perform(post("/api/feed-items").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }
}
