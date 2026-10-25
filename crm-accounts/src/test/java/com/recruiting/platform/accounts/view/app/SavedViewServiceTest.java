package com.recruiting.platform.accounts.view.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recruiting.platform.accounts.domain.SavedView;
import com.recruiting.platform.accounts.domain.SavedViewRepository;
import com.recruiting.platform.accounts.shared.Actor;
import com.recruiting.platform.accounts.shared.Patch;
import com.recruiting.platform.accounts.view.web.dto.SavedViewCreateRequest;
import com.recruiting.platform.accounts.view.web.dto.SavedViewUpdateRequest;
import com.recruiting.platform.accounts.view.web.dto.ViewDefinition;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.ListEntity;
import com.recruiting.platform.contracts.ViewVisibility;
import com.recruiting.platform.security.UserDirectory;
import com.recruiting.platform.security.UserRef;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SavedViewServiceTest {

    private final SavedViewRepository repo = mock(SavedViewRepository.class);
    private final UserDirectory users = mock(UserDirectory.class);
    private final SavedViewService service = new SavedViewService(repo, users);
    private final Actor consultant = new Actor(UUID.randomUUID(), false);
    private final Actor manager = new Actor(UUID.randomUUID(), true);
    private final ViewDefinition definition = new ViewDefinition(Map.of("q", "x"), List.of("name"), List.of(), null);

    @BeforeEach
    void stubs() {
        lenient().when(repo.saveAndFlush(any(SavedView.class))).thenAnswer(i -> {
            SavedView saved = i.getArgument(0);
            if (saved.getVersion() == null) {
                ReflectionTestUtils.setField(saved, "version", 0L);
            }
            return saved;
        });
        lenient().when(users.ref(any())).thenReturn(Optional.of(new UserRef(UUID.randomUUID(), "Owner")));
    }

    private SavedView view(UUID owner, ViewVisibility visibility) {
        SavedView v = new SavedView();
        ReflectionTestUtils.setField(v, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(v, "version", 3L);
        v.setEntity(ListEntity.ACCOUNTS);
        v.setName("Mine");
        v.setOwnerId(owner);
        v.setVisibility(visibility);
        v.setDefinition(SavedViewService.toMap(definition));
        when(repo.findById(v.getId())).thenReturn(Optional.of(v));
        return v;
    }

    private static Patch<SavedViewUpdateRequest> patch(SavedViewUpdateRequest body, String... present) {
        return new Patch<>(body, Set.of(present));
    }

    private static void assertCode(Runnable r, ErrorCode code) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    @Test
    void consultantCannotCreateSharedView() {
        assertCode(() -> service.create(new SavedViewCreateRequest(ListEntity.ACCOUNTS, "A", ViewVisibility.SHARED,
                definition), consultant), ErrorCode.FORBIDDEN);
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void managerCreatesSharedViewAndOwnsIt() {
        var created = service.create(new SavedViewCreateRequest(ListEntity.ACCOUNTS, " A ", ViewVisibility.SHARED,
                definition), manager);
        assertThat(created.name()).isEqualTo("A");
        assertThat(created.editable()).isTrue();
    }

    @Test
    void duplicateNameIsConflict() {
        SavedView existing = view(consultant.id(), ViewVisibility.PRIVATE);
        when(repo.findByOwnerAndName(consultant.id(), ListEntity.ACCOUNTS, "Mine")).thenReturn(Optional.of(existing));
        assertCode(() -> service.create(new SavedViewCreateRequest(ListEntity.ACCOUNTS, "Mine", null, definition),
                consultant), ErrorCode.DUPLICATE);
    }

    @Test
    void anotherUsersPrivateViewIsNotFound() {
        SavedView v = view(manager.id(), ViewVisibility.PRIVATE);
        assertCode(() -> service.update(v.getId(), patch(new SavedViewUpdateRequest("X", null, null, null), "name"),
                consultant), ErrorCode.NOT_FOUND);
        assertCode(() -> service.delete(v.getId(), consultant), ErrorCode.NOT_FOUND);
    }

    @Test
    void consultantCannotChangeOrDeleteSomeoneElsesSharedView() {
        SavedView v = view(manager.id(), ViewVisibility.SHARED);
        assertCode(() -> service.update(v.getId(), patch(new SavedViewUpdateRequest("X", null, null, null), "name"),
                consultant), ErrorCode.FORBIDDEN);
        assertCode(() -> service.delete(v.getId(), consultant), ErrorCode.FORBIDDEN);
    }

    @Test
    void managerChangesAndDeletesSharedViewOfOthers() {
        SavedView v = view(consultant.id(), ViewVisibility.SHARED);
        var updated = service.update(v.getId(), patch(new SavedViewUpdateRequest("Renamed", null, null, 3L), "name",
                "version"), manager);
        assertThat(updated.name()).isEqualTo("Renamed");
        service.delete(v.getId(), manager);
        verify(repo).delete(v);
    }

    @Test
    void consultantCannotSwitchOwnViewToShared() {
        SavedView v = view(consultant.id(), ViewVisibility.PRIVATE);
        assertCode(() -> service.update(v.getId(), patch(new SavedViewUpdateRequest(null, ViewVisibility.SHARED, null,
                null), "visibility"), consultant), ErrorCode.FORBIDDEN);
    }

    @Test
    void staleVersionIsConflict() {
        SavedView v = view(consultant.id(), ViewVisibility.PRIVATE);
        assertCode(() -> service.update(v.getId(), patch(new SavedViewUpdateRequest("X", null, null, 2L), "name",
                "version"), consultant), ErrorCode.VERSION_CONFLICT);
    }

    @Test
    void emptyPatchIsValidationError() {
        SavedView v = view(consultant.id(), ViewVisibility.PRIVATE);
        assertCode(() -> service.update(v.getId(), patch(new SavedViewUpdateRequest(null, null, null, null)),
                consultant), ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void listMergesOwnAndSharedSortedByName() {
        SavedView a = view(consultant.id(), ViewVisibility.PRIVATE);
        a.setName("b");
        SavedView b = view(manager.id(), ViewVisibility.SHARED);
        b.setName("A");
        when(repo.findVisible(ListEntity.ACCOUNTS, consultant.id(), ViewVisibility.SHARED)).thenReturn(List.of(a, b));
        when(users.refs(any())).thenReturn(Map.of());
        var result = service.list(ListEntity.ACCOUNTS, consultant);
        assertThat(result).extracting("name").containsExactly("A", "b");
        assertThat(result).extracting("editable").containsExactly(false, true);
    }
}
