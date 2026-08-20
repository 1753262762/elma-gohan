package com.elma.gohan.provider.deep;

import static org.assertj.core.api.Assertions.assertThat;

import com.elma.gohan.TestRestaurants;
import com.elma.gohan.config.DeepEvidenceProperties;
import com.elma.gohan.provider.evidence.EvidenceStatus;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BraveWebEvidenceProviderTest {

    private HttpServer server;
    private final AtomicReference<String> request = new AtomicReference<>();
    private final AtomicReference<String> token = new AtomicReference<>();
    private final AtomicReference<String> accept = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::respond);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void mapsOnlyWhitelistedRelevantResultsAndSendsFixedParameters() {
        DeepEvidenceProperties properties = properties();
        BraveWebEvidenceProvider provider = new BraveWebEvidenceProvider(properties,
                new WebEvidenceMatcher(properties));

        DeepEvidenceBatch result = provider.fetch(DeepEvidenceSource.BILIBILI,
                TestRestaurants.full("a", "老王湘菜馆", 4.6, 500, 42));

        assertThat(result.status()).isEqualTo(EvidenceStatus.AVAILABLE);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).url())
                .isEqualTo("https://www.bilibili.com/video/BV123");
        assertThat(result.items().get(0).publishedAt().toString())
                .isEqualTo("2026-08-10T09:30:00Z");
        String decoded = URLDecoder.decode(request.get(), StandardCharsets.UTF_8);
        assertThat(decoded).contains("/res/v1/web/search?")
                .contains("site:bilibili.com/video")
                .contains("freshness=pm")
                .contains("country=CN")
                .contains("search_lang=zh-hans")
                .contains("spellcheck=false")
                .contains("extra_snippets=false");
        assertThat(token.get()).isEqualTo("test-brave-key");
        assertThat(accept.get()).isEqualTo("application/json");
    }

    @Test
    void missingKeyDegradesWithoutRequest() {
        DeepEvidenceProperties properties = properties();
        properties.setApiKey("");
        BraveWebEvidenceProvider provider = new BraveWebEvidenceProvider(properties,
                new WebEvidenceMatcher(properties));

        assertThat(provider.fetch(DeepEvidenceSource.DIANPING,
                TestRestaurants.full("a", 4.5, 100)).status())
                .isEqualTo(EvidenceStatus.UNAVAILABLE);
        assertThat(request.get()).isNull();
    }

    private DeepEvidenceProperties properties() {
        DeepEvidenceProperties properties = new DeepEvidenceProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-brave-key");
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setConnectTimeoutMs(500);
        properties.setReadTimeoutMs(500);
        return properties;
    }

    private void respond(HttpExchange exchange) throws IOException {
        request.set(exchange.getRequestURI().toString());
        token.set(exchange.getRequestHeaders().getFirst("X-Subscription-Token"));
        accept.set(exchange.getRequestHeaders().getFirst("Accept"));
        byte[] body = """
                {"web":{"results":[
                  {"title":"老王湘菜馆值得吃吗","url":"https://www.bilibili.com/video/BV123?utm_source=test","description":"麓山南路，分量足但高峰期排队","page_age":"2026-08-10T09:30:00"},
                  {"title":"老王湘菜馆错误域名","url":"https://example.com/video/1","description":"无关"},
                  {"title":"另一家店","url":"https://www.bilibili.com/video/BV999","description":"完全无关"}
                ]}}
                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
