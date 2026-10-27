package com.recruiting.platform.accounts.domain;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.recruiting.platform.contracts.Mobility;
import com.recruiting.platform.contracts.SourceChannel;
import jakarta.persistence.*;
import com.recruiting.platform.contracts.ConsentStatus;
import com.recruiting.platform.contracts.PersonType;
import com.recruiting.platform.contracts.RetentionStatus;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/** Maps table {@code person}. Other modules reference it by id only (ADR 0001). */
@Entity
@Table(name = "person")
@Audited
public class Person extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private PersonType type;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "company", length = 200)
    private String company;

    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "linkedin_url", length = 300)
    private String linkedinUrl;

    @Column(name = "how_we_know")
    private String howWeKnow;

    @Column(name = "notes")
    private String notes;

    @Column(name = "owner_id")
    private UUID ownerId;

    @NotAudited
    @Column(name = "last_contact_at")
    private Instant lastContactAt;

    @NotAudited
    @Column(name = "consent_status")
    @Enumerated(EnumType.STRING)
    private ConsentStatus consentStatus = ConsentStatus.NOT_SENT;

    @NotAudited
    @Column(name = "retention_status")
    @Enumerated(EnumType.STRING)
    private RetentionStatus retentionStatus = RetentionStatus.ACTIVE;

    @Column(name = "anonymised_at")
    private Instant anonymisedAt;

    /** Lower-case labels, at most 20 (ADR 0010); column {@code tags varchar(50)[]}. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", length = 50)
    private List<String> tags = new ArrayList<>();

    /** Not personal data: kept on anonymisation (1.2.0). */
    @Column(name = "source_channel")
    @Enumerated(EnumType.STRING)
    private SourceChannel sourceChannel;

    @Column(name = "notice_period_days")
    private Short noticePeriodDays;

    @Column(name = "mobility")
    @Enumerated(EnumType.STRING)
    private Mobility mobility;

    /** Stored encrypted (1 byte key version, 12 byte IV, ciphertext + tag) through security.FieldCipher. */
    @Convert(converter = CompensationEncryptionConverter.class)
    @Column(name = "compensation_enc")
    private Compensation compensation;

    /** [{language, level}], at most 20, validated by the service. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "languages")
    private List<Map<String, Object>> languages = new ArrayList<>();

    @Column(name = "phone_normalized", length = 20)
    private String phoneNormalized;

    @Column(name = "linkedin_url_normalized", length = 300)
    private String linkedinUrlNormalized;

    public Person() {
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

    public PersonType getType() {
        return type;
    }

    public void setType(PersonType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public String getHowWeKnow() {
        return howWeKnow;
    }

    public void setHowWeKnow(String howWeKnow) {
        this.howWeKnow = howWeKnow;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public Instant getLastContactAt() {
        return lastContactAt;
    }

    public void setLastContactAt(Instant lastContactAt) {
        this.lastContactAt = lastContactAt;
    }

    public ConsentStatus getConsentStatus() {
        return consentStatus;
    }

    public void setConsentStatus(ConsentStatus consentStatus) {
        this.consentStatus = consentStatus;
    }

    public RetentionStatus getRetentionStatus() {
        return retentionStatus;
    }

    public void setRetentionStatus(RetentionStatus retentionStatus) {
        this.retentionStatus = retentionStatus;
    }

    public Instant getAnonymisedAt() {
        return anonymisedAt;
    }

    public void setAnonymisedAt(Instant anonymisedAt) {
        this.anonymisedAt = anonymisedAt;
    }


    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }
    public SourceChannel getSourceChannel() {
        return sourceChannel;
    }

    public void setSourceChannel(SourceChannel sourceChannel) {
        this.sourceChannel = sourceChannel;
    }

    public Short getNoticePeriodDays() {
        return noticePeriodDays;
    }

    public void setNoticePeriodDays(Short noticePeriodDays) {
        this.noticePeriodDays = noticePeriodDays;
    }

    public Mobility getMobility() {
        return mobility;
    }

    public void setMobility(Mobility mobility) {
        this.mobility = mobility;
    }

    public Compensation getCompensation() {
        return compensation;
    }

    public void setCompensation(Compensation compensation) {
        this.compensation = compensation;
    }

    public List<Map<String, Object>> getLanguages() {
        return languages;
    }

    public void setLanguages(List<Map<String, Object>> languages) {
        this.languages = languages == null ? new ArrayList<>() : new ArrayList<>(languages);
    }

    public String getPhoneNormalized() {
        return phoneNormalized;
    }

    public void setPhoneNormalized(String phoneNormalized) {
        this.phoneNormalized = phoneNormalized;
    }

    public String getLinkedinUrlNormalized() {
        return linkedinUrlNormalized;
    }

    public void setLinkedinUrlNormalized(String linkedinUrlNormalized) {
        this.linkedinUrlNormalized = linkedinUrlNormalized;
    }

}
