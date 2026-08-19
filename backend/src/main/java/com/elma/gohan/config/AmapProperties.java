package com.elma.gohan.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 高德 Web Service 配置。key 只从环境变量 AMAP_KEY 读取,禁止写进仓库或日志。
 */
@ConfigurationProperties(prefix = "elma.amap")
public class AmapProperties {

    private String key = "";
    private String baseUrl = "https://restapi.amap.com";
    /** 高德分类码:餐饮服务大类。 */
    private String types = "050000";
    private int pageSize = 25;
    private int maxPages = 2;
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;
    /** 高德 typecode -> ELMA 内部品类(code 遵循 ^[A-Z][A-Z0-9_]{0,31}$)。未命中走 OTHER 兜底。 */
    private Map<String, CategoryMapping> categoryMap = Map.of();

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getTypes() { return types; }
    public void setTypes(String types) { this.types = types; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public int getMaxPages() { return maxPages; }
    public void setMaxPages(int maxPages) { this.maxPages = maxPages; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    public Map<String, CategoryMapping> getCategoryMap() { return categoryMap; }
    public void setCategoryMap(Map<String, CategoryMapping> categoryMap) { this.categoryMap = categoryMap; }

    public static class CategoryMapping {
        private String code;
        private String label;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
}
