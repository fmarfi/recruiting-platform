package com.recruiting.platform.security.audit.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.RevisionType;
import com.recruiting.platform.security.UserDirectory;
import com.recruiting.platform.security.audit.web.dto.RevisionResponse;
import com.recruiting.platform.security.audit.web.dto.RevisionResponse.FieldChange;
import com.recruiting.platform.security.domain.AuditRevision;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.SingularAttribute;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.envers.Audited;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Change history of one record from Envers: who changed what and when. The audited entity class is found by name in
 * the JPA metamodel, so this module needs no compile-time knowledge of the other modules' entities.
 */
@Service
public class RevisionQueryService {

    @PersistenceContext
    private EntityManager em;

    private final UserDirectory directory;

    RevisionQueryService(UserDirectory directory) {
        this.directory = directory;
    }

    /** All revisions of the record, newest first, as one page. */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public org.springframework.data.domain.Page<RevisionResponse> history(EntityType type, UUID id, int page,
            int size) {
        Class<?> entityClass = auditedClass(type);
        List<Object[]> rows = AuditReaderFactory.get(em).createQuery()
                .forRevisionsOfEntity(entityClass, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();
        List<String> fields = basicAttributes(entityClass);
        List<RevisionResponse> all = new ArrayList<>();
        Object previous = null;
        for (Object[] row : rows) {
            AuditRevision revision = (AuditRevision) row[1];
            var revisionType = RevisionType.valueOf(((org.hibernate.envers.RevisionType) row[2]).name());
            all.add(new RevisionResponse(revision.getId(), Instant.ofEpochMilli(revision.getTimestamp()),
                    directory.ref(revision.getUserId()).orElse(null), revisionType,
                    revisionType == RevisionType.DEL ? List.of() : diff(fields, previous, row[0])));
            previous = revisionType == RevisionType.DEL ? null : row[0];
        }
        Collections.reverse(all);
        int from = Math.min(Math.max(page, 0) * size, all.size());
        int to = Math.min(from + size, all.size());
        return new org.springframework.data.domain.PageImpl<>(all.subList(from, to),
                org.springframework.data.domain.PageRequest.of(Math.max(page, 0), size), all.size());
    }

    private Class<?> auditedClass(EntityType type) {
        String name = type == EntityType.USER ? "AppUser" : camel(type.name());
        return em.getMetamodel().getEntities().stream()
                .filter(e -> e.getName().equals(name) && e.getJavaType().isAnnotationPresent(Audited.class)
                        || e.getName().equals(name) && hasAuditedSuperclass(e.getJavaType()))
                .map(e -> (Class<?>) e.getJavaType())
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED,
                        "No change history is kept for " + type + ".",
                        java.util.Map.of("errors", List.of(java.util.Map.of("field", "entityType",
                                "code", "NOT_AUDITED", "message", "This type has no change history.")))));
    }

    private static boolean hasAuditedSuperclass(Class<?> type) {
        for (Class<?> c = type.getSuperclass(); c != null; c = c.getSuperclass()) {
            if (c.isAnnotationPresent(Audited.class)) {
                return true;
            }
        }
        return false;
    }

    private static String camel(String enumName) {
        StringBuilder out = new StringBuilder();
        for (String part : enumName.split("_")) {
            out.append(part.charAt(0)).append(part.substring(1).toLowerCase());
        }
        return out.toString();
    }

    private List<String> basicAttributes(Class<?> entityClass) {
        return em.getMetamodel().entity(entityClass).getSingularAttributes().stream()
                .filter(a -> a.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC)
                .map(SingularAttribute::getName)
                .filter(n -> !n.equals("version") && !n.equals("id"))
                .sorted()
                .toList();
    }

    private static List<FieldChange> diff(List<String> fields, Object before, Object after) {
        BeanWrapper afterBean = PropertyAccessorFactory.forBeanPropertyAccess(after);
        BeanWrapper beforeBean = before == null ? null : PropertyAccessorFactory.forBeanPropertyAccess(before);
        List<FieldChange> changes = new ArrayList<>();
        for (String field : fields) {
            if (!afterBean.isReadableProperty(field)) {
                continue;
            }
            Object to = plain(afterBean.getPropertyValue(field));
            Object from = beforeBean == null ? null : plain(beforeBean.getPropertyValue(field));
            if (!Objects.equals(from, to)) {
                changes.add(new FieldChange(field, from, to));
            }
        }
        return changes;
    }

    private static Object plain(Object value) {
        return value instanceof Enum<?> e ? e.name() : value;
    }
}
