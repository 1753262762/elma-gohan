package com.elma.gohan.provider.poi;

/** 上游 POI 服务不可用(网络错误、超时、业务失败),统一映射为 502 POI_PROVIDER_UNAVAILABLE。 */
public class PoiProviderException extends RuntimeException {

    public PoiProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public PoiProviderException(String message) {
        super(message);
    }
}
