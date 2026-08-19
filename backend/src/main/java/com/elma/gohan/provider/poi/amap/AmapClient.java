package com.elma.gohan.provider.poi.amap;

import com.elma.gohan.config.AmapProperties;
import com.elma.gohan.provider.poi.PoiProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 高德 Web Service 周边搜索客户端。Key 只来自 AmapProperties(环境变量 AMAP_KEY),
 * 请求 URL 含 Key,因此禁止把 URL 记入日志。
 */
@Component
public class AmapClient {

    private static final Logger log = LoggerFactory.getLogger(AmapClient.class);

    private final AmapProperties props;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AmapClient(AmapProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        if (props.getKey() == null || props.getKey().isBlank()) {
            log.warn("AMAP_KEY 未配置,推荐接口将返回 502 POI_PROVIDER_UNAVAILABLE");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(props.getReadTimeoutMs()));
        this.restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    /** 返回原始 poi 节点列表(未转换);分页最多 maxPages。 */
    public List<JsonNode> searchAround(double latitude, double longitude, int radiusMeters) {
        if (props.getKey() == null || props.getKey().isBlank()) {
            throw new PoiProviderException("AMAP key 未配置");
        }
        List<JsonNode> pois = new ArrayList<>();
        try {
            for (int page = 1; page <= props.getMaxPages(); page++) {
                final int currentPage = page;
                JsonNode body = restClient.get()
                        .uri(uriBuilder -> uriBuilder.path("/v3/place/around")
                                .queryParam("key", props.getKey())
                                .queryParam("location", longitude + "," + latitude)
                                .queryParam("radius", radiusMeters)
                                .queryParam("types", props.getTypes())
                                .queryParam("extensions", "all")
                                .queryParam("offset", props.getPageSize())
                                .queryParam("page", currentPage)
                                .build())
                        .retrieve()
                        .body(JsonNode.class);
                if (body == null || !"1".equals(body.path("status").asText())) {
                    throw new PoiProviderException("高德周边搜索返回失败状态");
                }
                JsonNode pagePois = body.path("pois");
                if (!pagePois.isArray()) {
                    break;
                }
                pagePois.forEach(pois::add);
                if (pagePois.size() < props.getPageSize()) {
                    break;
                }
            }
        } catch (RestClientException e) {
            log.warn("高德周边搜索请求失败: {}", e.getMessage());
            throw new PoiProviderException("高德周边搜索请求失败", e);
        }
        return pois;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
