package com.elma.gohan.provider.evidence;

/** 外部证据的可用状态，用于区分确实无数据和 Provider 故障。 */
public enum EvidenceStatus {
    AVAILABLE,
    NO_DATA,
    UNAVAILABLE
}
