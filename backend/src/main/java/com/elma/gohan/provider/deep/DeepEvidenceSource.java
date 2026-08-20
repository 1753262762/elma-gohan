package com.elma.gohan.provider.deep;

public enum DeepEvidenceSource {
    BILIBILI("site:bilibili.com/video"),
    XIAOHONGSHU("site:xiaohongshu.com"),
    DIANPING("site:dianping.com");

    private final String siteQuery;

    DeepEvidenceSource(String siteQuery) {
        this.siteQuery = siteQuery;
    }

    public String siteQuery() {
        return siteQuery;
    }
}
