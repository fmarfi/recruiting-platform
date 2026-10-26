package com.recruiting.platform.accounts.domain;

import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

/** Maps table {@code person_education} (1.2.0). Replaced as a whole on update, like person_employment. */
@Entity
@Table(name = "person_education")
@Audited
public class PersonEducation {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "school", length = 200)
    private String school;

    @Column(name = "degree", length = 100)
    private String degree;

    @Column(name = "field", length = 200)
    private String field;

    @Column(name = "start_year")
    private Short startYear;

    @Column(name = "end_year")
    private Short endYear;

    @Column(name = "sort_order")
    private Integer sortOrder;

    public PersonEducation() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Short getStartYear() {
        return startYear;
    }

    public void setStartYear(Short startYear) {
        this.startYear = startYear;
    }

    public Short getEndYear() {
        return endYear;
    }

    public void setEndYear(Short endYear) {
        this.endYear = endYear;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
