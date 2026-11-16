package com.recruiting.platform.pipeline.agreement.app;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Read-only facts about accounts and contacts that the agreement rules need (archived, VAT, active contact). */
@Component
class AccountFacts {

    /** Archive and VAT flags of an account. */
    record Flags(boolean archived, boolean vatExempt) {
    }

    @PersistenceContext
    private EntityManager em;

    Flags flags(UUID accountId) {
        List<?> rows = em.createNativeQuery("select archived_at is not null, vat_exempt from account where id = :id")
                .setParameter("id", accountId).getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        Object[] r = (Object[]) rows.get(0);
        return new Flags(Boolean.TRUE.equals(r[0]), Boolean.TRUE.equals(r[1]));
    }

    boolean isActiveContactOf(UUID contactId, UUID accountId) {
        return !em.createNativeQuery("select 1 from contact where id = :c and account_id = :a and ended_on is null")
                .setParameter("c", contactId).setParameter("a", accountId).getResultList().isEmpty();
    }

    /** Person id per contact id. */
    Map<UUID, UUID> personIds(Collection<UUID> contactIds) {
        Map<UUID, UUID> out = new HashMap<>();
        if (contactIds.isEmpty()) {
            return out;
        }
        for (Object o : em.createNativeQuery("select id, person_id from contact where id in (:ids)")
                .setParameter("ids", contactIds).getResultList()) {
            Object[] r = (Object[]) o;
            out.put((UUID) r[0], (UUID) r[1]);
        }
        return out;
    }
}
