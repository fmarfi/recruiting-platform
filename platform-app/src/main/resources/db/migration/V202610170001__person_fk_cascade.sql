-- ADR 0009: one rule for foreign keys to person.
-- Every row that only exists about a person is deleted with the person (ON DELETE CASCADE).
-- The only exception is data_subject_request.person_id (ON DELETE SET NULL): the request is kept, minimised,
-- as evidence that the request was handled.
-- Before this migration activity.person_id was SET NULL, which violated ck_activity_target for person-only
-- activities, so DELETE FROM person failed (S-03 retention action DELETE).
-- next_action.person_id was SET NULL too; its title may name the person, so it follows the same rule.

ALTER TABLE activity DROP CONSTRAINT activity_person_id_fkey;
ALTER TABLE activity
    ADD CONSTRAINT activity_person_id_fkey FOREIGN KEY (person_id) REFERENCES person (id) ON DELETE CASCADE;

ALTER TABLE next_action DROP CONSTRAINT next_action_person_id_fkey;
ALTER TABLE next_action
    ADD CONSTRAINT next_action_person_id_fkey FOREIGN KEY (person_id) REFERENCES person (id) ON DELETE CASCADE;
