package com.elma.gohan;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 接口集成测试:连本机 elma_test 库,用本地 HTTP stub 替代高德(不依赖真实 Key)。
 * 验证状态码与响应体形状与 contracts/openapi.yaml 一致。
 * stub 在静态块中启动,确保早于 Spring 上下文读取 elma.amap.base-url。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecommendationApiTest {

    private static final String USER = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER_USER = "22222222-2222-2222-2222-222222222222";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    private static final HttpServer amapStub;
    private static final AtomicReference<String> amapResponse = new AtomicReference<>("{}");

    static {
        try {
            amapStub = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            amapStub.createContext("/v3/place/around", exchange -> {
                byte[] body = amapResponse.get().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            amapStub.start();
            amapResponse.set(eightPois());
        } catch (IOException e) {
            throw new IllegalStateException("无法启动高德 stub", e);
        }
    }

    @DynamicPropertySource
    static void amapBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("elma.amap.base-url",
                () -> "http://localhost:" + amapStub.getAddress().getPort());
    }

    @AfterAll
    static void stopStub() {
        amapStub.stop(0);
    }

    @AfterEach
    void cleanDb() {
        jdbc.update("DELETE FROM user_preference");
        jdbc.update("DELETE FROM user_feedback");
        jdbc.update("DELETE FROM recommendation_candidate");
        jdbc.update("DELETE FROM recommendation_log");
        jdbc.update("DELETE FROM risk_result");
        jdbc.update("DELETE FROM restaurant");
    }

    @Test
    void createReturns201WithContractShape() throws Exception {
        ResponseEntity<String> response = create(USER, """
                {"latitude": 28.2282, "longitude": 112.9388, "radius": 1000,
                 "maxBudget": 40, "category": "ANY", "dislikes": []}
                """);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        JsonNode body = JSON.readTree(response.getBody());
        assertThat(body.get("recommendationId")).isNotNull();
        JsonNode restaurant = body.get("restaurant");
        assertThat(restaurant.get("id")).isNotNull();
        assertThat(restaurant.get("name").asText()).isNotBlank();
        assertThat(restaurant.get("category").get("code")).isNotNull();
        assertThat(restaurant.get("category").get("label")).isNotNull();
        assertThat(restaurant.get("distanceMeters").asInt()).isGreaterThanOrEqualTo(0);
        assertThat(restaurant.get("walkingMinutes").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(restaurant.get("businessStatus").asText()).isIn("OPEN", "CLOSED", "UNKNOWN");
        JsonNode risk = body.get("risk");
        assertThat(risk.get("riskScore").asInt()).isBetween(0, 100);
        assertThat(risk.get("riskLevel").asText()).isIn("LOW", "MEDIUM_LOW", "MEDIUM", "HIGH");
        assertThat(risk.get("reasons").size()).isGreaterThanOrEqualTo(1);
        assertThat(risk.get("algorithmVersion").asText()).isEqualTo("risk-v0.1");
        assertThat(body.get("reasons").size()).isBetween(1, 5);
        assertThat(body.get("alternativesRemaining").asInt()).isBetween(0, 2);
        // 落库校验:推荐日志含条件快照与双算法版本
        Integer logs = jdbc.queryForObject(
                "SELECT count(*) FROM recommendation_log WHERE request_condition_json IS NOT NULL "
                        + "AND risk_algorithm_version = 'risk-v0.1' "
                        + "AND recommendation_algorithm_version = 'lowregret-v0.1'", Integer.class);
        assertThat(logs).isEqualTo(1);
    }

    @Test
    void rerollFlowShowsAtMostThreeCandidatesThenBackToA() throws Exception {
        String createBody = create(USER, """
                {"latitude": 28.2282, "longitude": 112.9388}
                """).getBody();
        JsonNode created = JSON.readTree(createBody);
        String recommendationId = created.get("recommendationId").asText();
        assertThat(created.get("alternativesRemaining").asInt()).isEqualTo(2);

        String a = created.get("restaurant").get("id").asText();
        JsonNode second = reroll(USER, recommendationId);
        String b = second.get("restaurant").get("id").asText();
        assertThat(second.get("alternativesRemaining").asInt()).isEqualTo(1);
        JsonNode third = reroll(USER, recommendationId);
        String c = third.get("restaurant").get("id").asText();
        assertThat(third.get("alternativesRemaining").asInt()).isEqualTo(0);
        assertThat(a).isNotEqualTo(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(b).isNotEqualTo(c);

        // 耗尽后返回初始 A,不产生第四家
        JsonNode exhausted = reroll(USER, recommendationId);
        assertThat(exhausted.get("restaurant").get("id").asText()).isEqualTo(a);
        assertThat(exhausted.get("alternativesRemaining").asInt()).isEqualTo(0);
    }

    @Test
    void feedbackRecordsCurrentDisplayedRestaurant() throws Exception {
        String recommendationId = JSON.readTree(
                create(USER, "{\"latitude\": 28.2282, \"longitude\": 112.9388}").getBody())
                .get("recommendationId").asText();
        JsonNode rerolled = reroll(USER, recommendationId);
        String displayed = rerolled.get("restaurant").get("id").asText();

        ResponseEntity<String> response = post(
                "/api/v1/recommendations/glm-5.3_common/feedback".replace("glm-5.3_common", recommendationId),
                USER, "{\"result\": \"LIKE\"}");
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        JsonNode feedback = JSON.readTree(response.getBody());
        assertThat(feedback.get("result").asText()).isEqualTo("LIKE");
        assertThat(feedback.get("recommendationId").asText()).isEqualTo(recommendationId);
        assertThat(feedback.get("restaurantId").asText()).isEqualTo(displayed);
        assertThat(feedback.get("recordedAt")).isNotNull();
    }

    @Test
    void invalidRadiusReturns400WithFieldError() throws Exception {
        ResponseEntity<String> response = create(USER, """
                {"latitude": 28.2282, "longitude": 112.9388, "radius": 800}
                """);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        JsonNode body = JSON.readTree(response.getBody());
        assertThat(body.get("code").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(body.get("fieldErrors").get(0).get("field").asText()).isEqualTo("radius");
        assertThat(body.get("traceId")).isNotNull();
    }

    @Test
    void invalidUserHeaderReturns400() throws Exception {
        ResponseEntity<String> response = create("not-a-uuid",
                "{\"latitude\": 28.2282, \"longitude\": 112.9388}");
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(JSON.readTree(response.getBody()).get("code").asText())
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void missingHeaderReturns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = rest.postForEntity("/api/v1/recommendations",
                new HttpEntity<>("{\"latitude\": 28.2282, \"longitude\": 112.9388}", headers),
                String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void unknownRecommendationReturns404() {
        ResponseEntity<String> response = post(
                "/api/v1/recommendations/glm-5.3_common/reroll".replace("glm-5.3_common",
                        UUID.randomUUID().toString()), USER, null);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        try {
            assertThat(JSON.readTree(response.getBody()).get("code").asText())
                    .isEqualTo("RECOMMENDATION_NOT_FOUND");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void rerollFromAnotherUserReturns404() throws Exception {
        String recommendationId = JSON.readTree(
                create(USER, "{\"latitude\": 28.2282, \"longitude\": 112.9388}").getBody())
                .get("recommendationId").asText();
        ResponseEntity<String> response = post(
                "/api/v1/recommendations/glm-5.3_common/reroll".replace("glm-5.3_common", recommendationId),
                OTHER_USER, null);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void emptyPoisReturns422() {
        amapResponse.set("{\"status\":\"1\",\"pois\":[]}");
        try {
            ResponseEntity<String> response = create(USER,
                    "{\"latitude\": 28.2282, \"longitude\": 112.9388}");
            assertThat(response.getStatusCode().value()).isEqualTo(422);
            try {
                assertThat(JSON.readTree(response.getBody()).get("code").asText())
                        .isEqualTo("NO_RECOMMENDATION_AVAILABLE");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            amapResponse.set(eightPois());
        }
    }

    @Test
    void amapFailureReturns502() {
        amapResponse.set("{\"status\":\"0\",\"info\":\"INVALID_USER_KEY\"}");
        try {
            ResponseEntity<String> response = create(USER,
                    "{\"latitude\": 28.2282, \"longitude\": 112.9388}");
            assertThat(response.getStatusCode().value()).isEqualTo(502);
            try {
                assertThat(JSON.readTree(response.getBody()).get("code").asText())
                        .isEqualTo("POI_PROVIDER_UNAVAILABLE");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            amapResponse.set(eightPois());
        }
    }

    private ResponseEntity<String> create(String userId, String body) {
        return post("/api/v1/recommendations", userId, body);
    }

    private JsonNode reroll(String userId, String recommendationId) throws Exception {
        ResponseEntity<String> response = post(
                "/api/v1/recommendations/glm-5.3_common/reroll".replace("glm-5.3_common", recommendationId),
                userId, null);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return JSON.readTree(response.getBody());
    }

    private ResponseEntity<String> post(String path, String userId, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Anonymous-User-Id", userId);
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private static String eightPois() {
        StringBuilder pois = new StringBuilder("[");
        for (int i = 1; i <= 8; i++) {
            if (i > 1) {
                pois.append(',');
            }
            pois.append("""
                    {"id": "POI-%d", "name": "测试餐厅%d", "type": "餐饮服务;中餐厅;家常菜",
                     "typecode": "050100", "address": "麓山南路 %d 号",
                     "location": "112.9412,28.2291", "distance": "%d",
                     "pname": "湖南省", "cityname": "长沙市", "adname": "岳麓区",
                     "biz_ext": {"rating": "4.%d", "cost": "%d", "opening_time": "09:00-21:00"}}
                    """.formatted(i, i, i, 200 + i * 80, 9 - i, 20 + i));
        }
        pois.append(']');
        return "{\"status\":\"1\",\"info\":\"OK\",\"pois\":" + pois + "}";
    }
}
