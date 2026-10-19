package com.recruiting.platform.accounts.referencedata.web.dto;

import com.recruiting.platform.contracts.Tone;
import java.util.List;

/** One status with label, tone and allowed moves. */
public record StatusDefinition(String value, String labelEn, String labelTr, Tone tone, boolean terminal,
        List<TransitionDefinition> next) {
}
