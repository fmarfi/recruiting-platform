package com.recruiting.platform.accounts.shared.dto;

/** Paging block of list responses (ADR 0006). */
public record PageMeta(int size, int number, long totalElements, int totalPages) {
}
