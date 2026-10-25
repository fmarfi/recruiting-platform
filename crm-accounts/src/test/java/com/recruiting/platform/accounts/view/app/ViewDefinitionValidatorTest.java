package com.recruiting.platform.accounts.view.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruiting.platform.accounts.view.web.dto.ViewDefinition;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.ListEntity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ViewDefinitionValidatorTest {

    private static ViewDefinition def(Map<String, Object> filters, List<String> columns, List<String> sort,
            String groupBy) {
        return new ViewDefinition(filters, columns, sort, groupBy);
    }

    private static String firstField(ApiException e) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> errors = (List<Map<String, String>>) e.properties().get("errors");
        return errors.get(0).get("field");
    }

    @Test
    void acceptsTheContractExample() {
        ViewDefinition d = def(Map.of("status", List.of("ACTIVE", "ON_HOLD"), "ownerId", "me"),
                List.of("name", "status", "sector", "owner", "lastActivityAt"), List.of("lastActivityAt,desc"),
                "sector");
        assertThatCode(() -> ViewDefinitionValidator.validate(ListEntity.ACCOUNTS, d)).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnknownFilterKeyWithFieldName() {
        ViewDefinition d = def(Map.of("bogus", "x"), List.of(), List.of(), null);
        assertThatThrownBy(() -> ViewDefinitionValidator.validate(ListEntity.ACCOUNTS, d))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                    assertThat(firstField(e)).isEqualTo("definition.filters.bogus");
                });
    }

    @Test
    void filterKeysAreValidatedPerEntity() {
        ViewDefinition d = def(Map.of("sector", "Energy"), List.of(), List.of(), null);
        assertThatThrownBy(() -> ViewDefinitionValidator.validate(ListEntity.PEOPLE, d))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsBadEnumUuidAndDateValues() {
        for (Map<String, Object> filters : List.<Map<String, Object>>of(Map.of("status", "NOPE"),
                Map.of("ownerId", "not-a-uuid"), Map.of("status", List.of("ACTIVE", 5)))) {
            assertThatThrownBy(() -> ViewDefinitionValidator.validate(ListEntity.ACCOUNTS,
                    def(filters, List.of(), List.of(), null))).isInstanceOf(ApiException.class);
        }
        assertThatThrownBy(() -> ViewDefinitionValidator.validate(ListEntity.PEOPLE,
                def(Map.of("lastContactBefore", "yesterday"), List.of(), List.of(), null)))
                .isInstanceOf(ApiException.class);
        assertThatCode(() -> ViewDefinitionValidator.validate(ListEntity.PEOPLE,
                def(Map.of("lastContactBefore", "2026-10-01"), List.of(), List.of(), null))).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnknownColumnSortAndGroupBy() {
        assertThatThrownBy(() -> ViewDefinitionValidator.validate(ListEntity.ACCOUNTS,
                def(Map.of(), List.of("nope"), List.of(), null))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> ViewDefinitionValidator.validate(ListEntity.ACCOUNTS,
                def(Map.of(), List.of(), List.of("nope,asc"), null))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> ViewDefinitionValidator.validate(ListEntity.ACCOUNTS,
                def(Map.of(), List.of(), List.of(), "nope"))).isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsNestedFilterValuesAndTooManyFilters() {
        assertThatThrownBy(() -> ViewDefinitionValidator.validate(ListEntity.ACCOUNTS,
                def(Map.of("status", Map.of("a", "b")), List.of(), List.of(), null))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> ViewDefinitionValidator.validate(ListEntity.ACCOUNTS,
                def(Map.of("sector", List.of(List.of("x"))), List.of(), List.of(), null)))
                .isInstanceOf(ApiException.class);
    }
}
