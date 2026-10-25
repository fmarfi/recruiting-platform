package com.recruiting.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruiting.platform.accounts.domain.Account;
import com.recruiting.platform.accounts.domain.AccountRepository;
import com.recruiting.platform.accounts.domain.SavedView;
import com.recruiting.platform.accounts.domain.SavedViewRepository;
import com.recruiting.platform.contracts.AccountStatus;
import com.recruiting.platform.contracts.BulkActionType;
import com.recruiting.platform.contracts.BulkOutcome;
import com.recruiting.platform.contracts.BulkResultCode;
import com.recruiting.platform.contracts.ListEntity;
import com.recruiting.platform.contracts.ViewVisibility;
import com.recruiting.platform.pipeline.domain.BulkAction;
import com.recruiting.platform.pipeline.domain.BulkActionItem;
import com.recruiting.platform.pipeline.domain.BulkActionItemRepository;
import com.recruiting.platform.pipeline.domain.BulkActionRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Wave 2a tables: saved_view, bulk_action(_item) and tags (ADR 0010). */
class Wave2aRepositoryIT extends AbstractPersistenceIT {

    @Autowired
    private SavedViewRepository views;

    @Autowired
    private BulkActionRepository bulkActions;

    @Autowired
    private BulkActionItemRepository bulkItems;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private EntityManager em;

    @Autowired
    private JdbcTemplate jdbc;

    private SavedView view(UUID owner, ListEntity entity, String name, ViewVisibility visibility) {
        SavedView v = new SavedView();
        v.setOwnerId(owner);
        v.setEntity(entity);
        v.setName(name);
        v.setVisibility(visibility);
        v.setDefinition(Map.of("filters", Map.of("status", "ACTIVE"), "columns", List.of("name"), "groupBy", "owner"));
        return tx.execute(s -> views.save(v));
    }

    @Test
    void savedViewRoundTripWithJsonDefinitionAndVisibility() {
        UUID me = newUser().getId();
        UUID other = newUser().getId();
        SavedView mine = view(me, ListEntity.ACCOUNTS, "Mine", ViewVisibility.PRIVATE);
        SavedView shared = view(other, ListEntity.ACCOUNTS, "Shared", ViewVisibility.SHARED);
        view(other, ListEntity.ACCOUNTS, "Their private", ViewVisibility.PRIVATE);
        view(me, ListEntity.PEOPLE, "Other entity", ViewVisibility.PRIVATE);

        SavedView loaded = views.findById(mine.getId()).orElseThrow();
        assertThat(loaded.getDefinition()).containsKey("filters");
        assertThat(loaded.getVersion()).isNotNull();

        List<UUID> visible = views.findVisible(ListEntity.ACCOUNTS, me, ViewVisibility.SHARED).stream()
                .map(SavedView::getId).toList();
        assertThat(visible).contains(mine.getId(), shared.getId());
        assertThat(visible).noneMatch(id -> views.findById(id).orElseThrow().getEntity() != ListEntity.ACCOUNTS);
        assertThat(visible).doesNotContainAnyElementsOf(
                views.findAll().stream().filter(v -> "Their private".equals(v.getName())).map(SavedView::getId).toList());
    }

    @Test
    void savedViewNameIsUniquePerOwnerAndEntityIgnoringCase() {
        UUID me = newUser().getId();
        view(me, ListEntity.PEOPLE, "Hot leads", ViewVisibility.PRIVATE);
        assertThat(views.findByOwnerAndName(me, ListEntity.PEOPLE, "HOT LEADS")).isPresent();
        assertThatThrownBy(() -> view(me, ListEntity.PEOPLE, "hot LEADS", ViewVisibility.SHARED))
                .isInstanceOf(DataIntegrityViolationException.class);
        // same name for another entity or another owner is allowed
        view(me, ListEntity.ACCOUNTS, "Hot leads", ViewVisibility.PRIVATE);
        view(newUser().getId(), ListEntity.PEOPLE, "Hot leads", ViewVisibility.PRIVATE);
    }

    @Test
    void savedViewChecksRejectUnknownEnumValues() {
        UUID me = newUser().getId();
        assertThatThrownBy(() -> jdbc.update(
                "insert into saved_view (id, entity, name, visibility, owner_id, definition) "
                        + "values (?, 'TASKS', 'x', 'PRIVATE', ?, '{}'::jsonb)", UUID.randomUUID(), me))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "insert into saved_view (id, entity, name, visibility, owner_id, definition) "
                        + "values (?, 'PEOPLE', 'x', 'PUBLIC', ?, '{}'::jsonb)", UUID.randomUUID(), me))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private BulkAction bulk(UUID actor) {
        BulkAction b = new BulkAction();
        b.setEntity(ListEntity.PEOPLE);
        b.setAction(BulkActionType.ADD_TAG);
        b.setParams(Map.of("tag", "vip"));
        b.setActorId(actor);
        b.setStartedAt(Instant.now());
        b.setFinishedAt(Instant.now());
        b.setRequestedCount(3);
        b.setOkCount(1);
        b.setSkippedCount(1);
        b.setFailedCount(1);
        return tx.execute(s -> bulkActions.save(b));
    }

    @Test
    void bulkActionItemsStoreEveryOutcomeAndCascadeOnDelete() {
        UUID actor = newUser().getId();
        BulkAction b = bulk(actor);
        UUID ok = UUID.randomUUID();
        UUID skipped = UUID.randomUUID();
        UUID failed = UUID.randomUUID();
        tx.executeWithoutResult(s -> bulkItems.saveAll(List.of(
                new BulkActionItem(b.getId(), ok, BulkOutcome.OK, null, null),
                new BulkActionItem(b.getId(), skipped, BulkOutcome.SKIPPED, BulkResultCode.NO_CHANGE, "already tagged"),
                new BulkActionItem(b.getId(), failed, BulkOutcome.FAILED, BulkResultCode.INTERNAL_ERROR, null))));

        assertThat(bulkItems.findByBulkActionId(b.getId())).hasSize(3);
        assertThat(bulkItems.findByEntityId(skipped)).singleElement()
                .extracting(BulkActionItem::getCode).isEqualTo(BulkResultCode.NO_CHANGE);
        assertThat(bulkActions.findById(b.getId()).orElseThrow().getParams()).containsEntry("tag", "vip");

        tx.executeWithoutResult(s -> bulkActions.deleteById(b.getId()));
        assertThat(bulkItems.findByBulkActionId(b.getId())).isEmpty();
    }

    @Test
    void bulkActionItemCodeRuleAndDuplicatesAreEnforced() {
        BulkAction b = bulk(newUser().getId());
        // OK must not carry a code; SKIPPED/FAILED must
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> bulkItems.saveAndFlush(
                new BulkActionItem(b.getId(), UUID.randomUUID(), BulkOutcome.OK, BulkResultCode.NO_CHANGE, null))))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> bulkItems.saveAndFlush(
                new BulkActionItem(b.getId(), UUID.randomUUID(), BulkOutcome.FAILED, null, null))))
                .isInstanceOf(DataIntegrityViolationException.class);
        UUID dup = UUID.randomUUID();
        tx.executeWithoutResult(s -> bulkItems.saveAndFlush(
                new BulkActionItem(b.getId(), dup, BulkOutcome.OK, null, null)));
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> bulkItems.saveAndFlush(
                new BulkActionItem(b.getId(), dup, BulkOutcome.OK, null, null))))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "insert into bulk_action_item (bulk_action_id, entity_id, outcome, code) values (?, ?, 'SKIPPED', 'BOGUS')",
                b.getId(), UUID.randomUUID())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "insert into bulk_action (id, entity, action, params, actor_id, started_at, finished_at, "
                        + "requested_count, ok_count, skipped_count, failed_count) "
                        + "values (?, 'PEOPLE', 'DELETE_ALL', '{}'::jsonb, ?, now(), now(), 0, 0, 0, 0)",
                UUID.randomUUID(), b.getActorId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void upperCaseTagIsRejectedByTheDatabase() {
        assertThatThrownBy(() -> account(List.of("Upper")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Account account(List<String> tags) {
        Account a = new Account();
        a.setName("Tagged " + UUID.randomUUID());
        a.setSector("Energy");
        a.setOwnerId(newUser().getId());
        a.setStatus(AccountStatus.LEAD);
        a.setStatusChangedAt(Instant.now());
        a.setOpenedOn(LocalDate.now());
        a.setOpenJobCount(0);
        a.setTags(tags);
        return tx.execute(s -> accounts.save(a));
    }

    @Test
    void tagsRoundTripAreSearchableAndAudited() {
        Account a = account(List.of("vip", "energy"));
        assertThat(accounts.findById(a.getId()).orElseThrow().getTags()).containsExactly("vip", "energy");
        assertThat(jdbc.queryForObject("select count(*) from account where id = ? and tags @> ARRAY['vip']::varchar[]",
                Integer.class, a.getId())).isEqualTo(1);
        assertThat(account(List.of()).getTags()).isEmpty();

        tx.executeWithoutResult(s -> {
            Account loaded = accounts.findById(a.getId()).orElseThrow();
            loaded.setTags(new ArrayList<>(List.of("vip")));
        });
        tx.executeWithoutResult(s -> {
            var reader = AuditReaderFactory.get(em);
            List<Number> revs = reader.getRevisions(Account.class, a.getId());
            assertThat(revs).hasSize(2);
            Account first = reader.find(Account.class, a.getId(), revs.get(0));
            assertThat(first.getTags()).containsExactly("vip", "energy");
        });
    }

    @Test
    void moreThanTwentyTagsAreRejected() {
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            tags.add("t" + i);
        }
        assertThatThrownBy(() -> account(tags)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void tagsColumnsAndIndexesExistOnAllFourTablesAndAudTables() {
        for (String table : List.of("person", "account", "job_order", "proposal")) {
            assertThat(jdbc.queryForObject("select count(*) from information_schema.columns "
                    + "where table_name in (?, ?) and column_name = 'tags'", Integer.class, table, table + "_aud"))
                    .as(table).isEqualTo(2);
            assertThat(jdbc.queryForObject("select count(*) from pg_indexes where tablename = ? "
                    + "and indexdef like '%USING gin (tags)%'", Integer.class, table)).as(table).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from pg_constraint where conrelid = ?::regclass "
                    + "and contype = 'c' and pg_get_constraintdef(oid) like '%cardinality(tags) <= 20%'",
                    Integer.class, table)).as(table).isEqualTo(1);
        }
    }
}
