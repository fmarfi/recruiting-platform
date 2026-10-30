package com.recruiting.platform.accounts.domain;

import com.recruiting.platform.contracts.TagColour;
import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

/** Maps table {@code tag_definition} (1.3.0, ADR 0013): name, colour and talent-pool flag of a tag. A tag exists with or without a row. */
@Entity
@Table(name = "tag_definition")
@Audited
public class TagDefinition extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "name", length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "colour")
    private TagColour colour = TagColour.GREY;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "talent_pool")
    private boolean talentPool;

    public TagDefinition() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public TagColour getColour() {
        return colour;
    }
    public void setColour(TagColour colour) {
        this.colour = colour;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isTalentPool() {
        return talentPool;
    }
    public void setTalentPool(boolean talentPool) {
        this.talentPool = talentPool;
    }

}
