package com.recruiting.platform.pipeline.bulk.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.BulkActionType;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.ListEntity;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class BulkRequestParserTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final String id = UUID.randomUUID().toString();

    private BulkRequestParser.Parsed parse(String entity, String action, String params) {
        return BulkRequestParser.parse(JSON.readTree("{\"entity\":\"" + entity + "\",\"action\":\"" + action
                + "\",\"ids\":[\"" + id + "\"],\"params\":" + params + "}"));
    }

    private void invalid(String entity, String action, String params) {
        assertThatThrownBy(() -> parse(entity, action, params)).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void tagIsLowerCasedAndConsentLanguageDefaultsToTurkish() {
        assertThat(parse("JOB_ORDERS", "ADD_TAG", "{\"tag\":\"CFO-Network\"}").params().tag()).isEqualTo("cfo-network");
        var consent = parse("PEOPLE", "REQUEST_CONSENT", "{}");
        assertThat(consent.action()).isEqualTo(BulkActionType.REQUEST_CONSENT);
        assertThat(consent.params().language()).isEqualTo(Language.TR);
    }

    @Test
    void reasonIsTrimmedAndKeptForTheRecordsOnly() {
        var p = parse("JOB_ORDERS", "CHANGE_STATUS", "{\"status\":\"CANCELLED\",\"reason\":\"  No budget left \"}");
        assertThat(p.params().reason()).isEqualTo("No budget left");
        assertThat(BulkActionService.auditParams(p.params())).containsOnlyKeys("status");
    }

    @Test
    void auditParamsNeverHoldTheTitle() {
        var p = parse("ACCOUNTS", "SET_NEXT_ACTION", "{\"title\":\"Call Ayse\",\"dueOn\":\"2030-01-01\"}");
        assertThat(p.params().title()).isEqualTo("Call Ayse");
        assertThat(BulkActionService.auditParams(p.params())).containsOnlyKeys("dueOn");
    }

    @Test
    void matrixAndShapeErrorsAreValidationFailures() {
        invalid("PEOPLE", "CHANGE_STATUS", "{\"status\":\"ACTIVE\"}");
        invalid("PROPOSALS", "SET_NEXT_ACTION", "{\"title\":\"x\",\"dueOn\":\"2030-01-01\"}");
        invalid("ACCOUNTS", "REQUEST_CONSENT", "{}");
        invalid("JOB_ORDERS", "CHANGE_STATUS", "{\"status\":\"ACCEPTED\"}");
        invalid("JOB_ORDERS", "CHANGE_STATUS", "{\"status\":\"CANCELLED\",\"reason\":\"ab\"}");
        invalid("JOB_ORDERS", "ADD_TAG", "{\"tag\":\"a\",\"extra\":true}");
        invalid("JOB_ORDERS", "ADD_TAG", "{\"tag\":\"\"}");
        invalid("JOB_ORDERS", "ADD_TAG", "{}");
        invalid("NOPE", "ADD_TAG", "{\"tag\":\"a\"}");
        assertThat(BulkRequestParser.allowed(ListEntity.ACCOUNTS, BulkActionType.CHANGE_STATUS)).isTrue();
    }
}
