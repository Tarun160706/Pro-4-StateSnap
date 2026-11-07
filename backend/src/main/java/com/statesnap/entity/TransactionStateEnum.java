package com.statesnap.entity;

public enum TransactionStateEnum {
    INITIATED,
    PROCESSING,
    COMMITTED,
    FAILED,
    ROLLED_BACK,
    UNKNOWN,
    CONFLICTED
}

