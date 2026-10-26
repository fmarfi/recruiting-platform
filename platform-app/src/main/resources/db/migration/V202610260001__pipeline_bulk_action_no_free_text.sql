-- Contract 1.1.1 (ADR 0010 amendment 1): the bulk audit holds ids and codes only, no free text.
-- bulk_action_item.message is dropped (the response message is BulkResultCode.message(), a fixed text per code).
ALTER TABLE bulk_action_item DROP COLUMN message;

-- bulk_action.params must not carry the free-text keys 'title' (SET_NEXT_ACTION) or 'reason' (CHANGE_STATUS).
-- Rows written before 1.1.1 may still have them: strip first, then add the CHECK.
UPDATE bulk_action SET params = params - 'title' - 'reason' WHERE params ?| array['title', 'reason'];
ALTER TABLE bulk_action
    ADD CONSTRAINT ck_bulk_action_params_no_free_text CHECK (NOT (params ?| array['title', 'reason']));
