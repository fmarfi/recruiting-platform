package com.recruiting.platform.accounts.referencedata.web.dto;

/** Allowed next status with its rules. */
public record TransitionDefinition(String to, boolean reasonRequired, boolean managerOnly) {
}
