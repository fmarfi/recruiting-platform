package com.recruiting.platform.accounts.importing.web.dto;

/** One row problem of an import (1-based data row). */
public record ImportRowError(int row, String column, String message) {
}
