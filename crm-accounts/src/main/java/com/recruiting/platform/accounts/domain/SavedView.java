package com.recruiting.platform.accounts.domain;

import com.recruiting.platform.contracts.ListEntity;
import com.recruiting.platform.contracts.ViewVisibility;
import jakarta.persistence.*;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/** Maps table {@code saved_view} (ADR 0010). Not audited: a user setting, not a business record. */
@Entity
@Table(name = "saved_view")
public class SavedView extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "entity")
    @Enumerated(EnumType.STRING)
    private ListEntity entity;

    @Column(name = "name", length = 120)
    private String name;

    @Column(name = "visibility")
    @Enumerated(EnumType.STRING)
    private ViewVisibility visibility = ViewVisibility.PRIVATE;

    @Column(name = "owner_id")
    private UUID ownerId;

    /** {filters, columns, sort, groupBy}; validated by the service per entity. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "definition")
    private Map<String, Object> definition;

    public SavedView() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public ListEntity getEntity() {
        return entity;
    }

    public void setEntity(ListEntity entity) {
        this.entity = entity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ViewVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(ViewVisibility visibility) {
        this.visibility = visibility;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public Map<String, Object> getDefinition() {
        return definition;
    }

    public void setDefinition(Map<String, Object> definition) {
        this.definition = definition;
    }

}
