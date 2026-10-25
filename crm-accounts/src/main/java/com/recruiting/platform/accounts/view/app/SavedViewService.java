package com.recruiting.platform.accounts.view.app;

import com.recruiting.platform.accounts.domain.SavedView;
import com.recruiting.platform.accounts.domain.SavedViewRepository;
import com.recruiting.platform.accounts.shared.Actor;
import com.recruiting.platform.accounts.shared.Patch;
import com.recruiting.platform.accounts.shared.PatchBinder;
import com.recruiting.platform.accounts.shared.Versions;
import com.recruiting.platform.accounts.view.web.dto.SavedViewCreateRequest;
import com.recruiting.platform.accounts.view.web.dto.SavedViewResponse;
import com.recruiting.platform.accounts.view.web.dto.SavedViewUpdateRequest;
import com.recruiting.platform.accounts.view.web.dto.ViewDefinition;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.ListEntity;
import com.recruiting.platform.contracts.ViewVisibility;
import com.recruiting.platform.security.UserDirectory;
import com.recruiting.platform.security.UserRef;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saved list views (ADR 0010). Users see their own PRIVATE views and all SHARED views. Another user's PRIVATE view
 * is a 404. SHARED views are created, switched to and changed by MANAGER/ADMIN (and by their owner when already
 * shared); anyone else gets 403. Views are user settings: no audit, no timeline.
 */
@Service
@Transactional
public class SavedViewService {

    static final int MAX_PRIVATE_PER_ENTITY = 50;

    private final SavedViewRepository views;
    private final UserDirectory users;

    public SavedViewService(SavedViewRepository views, UserDirectory users) {
        this.views = views;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<SavedViewResponse> list(ListEntity entity, Actor actor) {
        List<SavedView> found = new ArrayList<>();
        for (ListEntity e : entity == null ? ListEntity.values() : new ListEntity[] {entity}) {
            found.addAll(views.findVisible(e, actor.id(), ViewVisibility.SHARED));
        }
        found.sort(Comparator.comparing((SavedView v) -> v.getName().toLowerCase()).thenComparing(SavedView::getId));
        Map<UUID, UserRef> owners = users.refs(found.stream().map(SavedView::getOwnerId).toList());
        return found.stream().map(v -> response(v, owners.get(v.getOwnerId()), actor)).toList();
    }

    public SavedViewResponse create(SavedViewCreateRequest r, Actor actor) {
        ViewVisibility visibility = r.visibility() == null ? ViewVisibility.PRIVATE : r.visibility();
        String name = r.name().trim();
        if (name.isEmpty()) {
            throw PatchBinder.validation("name", "NotBlank", "must not be blank");
        }
        if (visibility == ViewVisibility.SHARED && !actor.elevated()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only a manager or admin may create a shared view.");
        }
        ViewDefinitionValidator.validate(r.entity(), r.definition());
        if (visibility == ViewVisibility.PRIVATE
                && views.countByOwnerIdAndEntityAndVisibility(actor.id(), r.entity(), ViewVisibility.PRIVATE)
                        >= MAX_PRIVATE_PER_ENTITY) {
            throw PatchBinder.validation("entity", "LIMIT",
                    "At most " + MAX_PRIVATE_PER_ENTITY + " private views per grid.");
        }
        failIfNameTaken(actor.id(), r.entity(), name, null);
        SavedView v = new SavedView();
        v.setEntity(r.entity());
        v.setName(name);
        v.setVisibility(visibility);
        v.setOwnerId(actor.id());
        v.setDefinition(toMap(r.definition()));
        return response(save(v), actor);
    }

    public SavedViewResponse update(UUID id, Patch<SavedViewUpdateRequest> patch, Actor actor) {
        SavedViewUpdateRequest b = patch.body();
        if (patch.present().isEmpty()) {
            throw PatchBinder.validation("body", "EMPTY", "at least one property is required");
        }
        PatchBinder.notNull(patch, "name", b.name());
        PatchBinder.notNull(patch, "visibility", b.visibility());
        PatchBinder.notNull(patch, "definition", b.definition());
        SavedView v = find(id, actor);
        boolean owner = actor.id().equals(v.getOwnerId());
        if (v.getVisibility() == ViewVisibility.SHARED && !owner && !actor.elevated()) {
            throw forbidden();
        }
        if (patch.has("visibility") && b.visibility() == ViewVisibility.SHARED
                && v.getVisibility() != ViewVisibility.SHARED && !actor.elevated()) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only a manager or admin may share a view.");
        }
        Versions.check(b.version(), v.getVersion());
        if (patch.has("name")) {
            String name = b.name().trim();
            if (name.isEmpty()) {
                throw PatchBinder.validation("name", "NotBlank", "must not be blank");
            }
            if (!name.equalsIgnoreCase(v.getName())) {
                failIfNameTaken(v.getOwnerId(), v.getEntity(), name, v.getId());
            }
            v.setName(name);
        }
        if (patch.has("visibility")) {
            v.setVisibility(b.visibility());
        }
        if (patch.has("definition")) {
            ViewDefinitionValidator.validate(v.getEntity(), b.definition());
            v.setDefinition(toMap(b.definition()));
        }
        return response(save(v), actor);
    }

    public void delete(UUID id, Actor actor) {
        SavedView v = find(id, actor);
        if (v.getVisibility() == ViewVisibility.SHARED && !actor.id().equals(v.getOwnerId()) && !actor.elevated()) {
            throw forbidden();
        }
        views.delete(v);
        views.flush();
    }

    private SavedView find(UUID id, Actor actor) {
        SavedView v = views.findById(id).orElseThrow(() -> notFound(id));
        if (v.getVisibility() == ViewVisibility.PRIVATE && !actor.id().equals(v.getOwnerId())) {
            throw notFound(id);
        }
        return v;
    }

    private SavedView save(SavedView v) {
        try {
            return views.saveAndFlush(v);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.DUPLICATE, "You already have a view with this name for this grid.");
        }
    }

    private void failIfNameTaken(UUID ownerId, ListEntity entity, String name, UUID selfId) {
        views.findByOwnerAndName(ownerId, entity, name).filter(o -> !o.getId().equals(selfId)).ifPresent(o -> {
            throw new ApiException(ErrorCode.DUPLICATE, "You already have a view with this name for this grid.",
                    Map.of("existingId", o.getId()));
        });
    }

    private static ApiException notFound(UUID id) {
        return new ApiException(ErrorCode.NOT_FOUND, "View " + id + " not found.");
    }

    private static ApiException forbidden() {
        return new ApiException(ErrorCode.FORBIDDEN, "Only the owner, a manager or an admin may change a shared view.");
    }

    private SavedViewResponse response(SavedView v, Actor actor) {
        return response(v, users.ref(v.getOwnerId()).orElse(null), actor);
    }

    private static SavedViewResponse response(SavedView v, UserRef owner, Actor actor) {
        boolean editable = actor.id().equals(v.getOwnerId())
                || (v.getVisibility() == ViewVisibility.SHARED && actor.elevated());
        return new SavedViewResponse(v.getId(), v.getEntity(), v.getName(), v.getVisibility(), owner,
                fromMap(v.getDefinition()), editable, v.getCreatedAt(), v.getUpdatedAt(), v.getVersion());
    }

    static Map<String, Object> toMap(ViewDefinition d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("filters", new LinkedHashMap<>(d.filters()));
        m.put("columns", new ArrayList<>(d.columns()));
        m.put("sort", new ArrayList<>(d.sort()));
        m.put("groupBy", d.groupBy());
        return m;
    }

    @SuppressWarnings("unchecked")
    static ViewDefinition fromMap(Map<String, Object> m) {
        Object filters = m.get("filters");
        Object columns = m.get("columns");
        Object sort = m.get("sort");
        return new ViewDefinition(filters == null ? Map.of() : (Map<String, Object>) filters,
                columns == null ? List.of() : (List<String>) columns, sort == null ? List.of() : (List<String>) sort,
                (String) m.get("groupBy"));
    }
}
