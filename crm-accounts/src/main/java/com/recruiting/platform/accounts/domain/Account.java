package com.recruiting.platform.accounts.domain;

import com.recruiting.platform.contracts.ArchiveReason;
import com.recruiting.platform.contracts.ConfidentialityLevel;
import com.recruiting.platform.contracts.EInvoiceStatus;
import com.recruiting.platform.contracts.EmployeeBand;
import com.recruiting.platform.contracts.Language;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import com.recruiting.platform.contracts.AccountStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/** Maps table {@code account}. Other modules reference it by id only (ADR 0001). */
@Entity
@Table(name = "account")
@Audited
public class Account extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "sector", length = 100)
    private String sector;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "website", length = 300)
    private String website;

    @Column(name = "notes")
    private String notes;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.LEAD;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt = Instant.now();

    @Column(name = "opened_on")
    private LocalDate openedOn = LocalDate.now();

    @NotAudited
    @Column(name = "open_job_count")
    private Integer openJobCount = 0;

    @NotAudited
    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    /** Lower-case labels, at most 20 (ADR 0010); column {@code tags varchar(50)[]}. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", length = 50)
    private List<String> tags = new ArrayList<>();

    public Account() {
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

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
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

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Instant getStatusChangedAt() {
        return statusChangedAt;
    }

    public void setStatusChangedAt(Instant statusChangedAt) {
        this.statusChangedAt = statusChangedAt;
    }

    public LocalDate getOpenedOn() {
        return openedOn;
    }

    public void setOpenedOn(LocalDate openedOn) {
        this.openedOn = openedOn;
    }

    public Integer getOpenJobCount() {
        return openJobCount;
    }

    public void setOpenJobCount(Integer openJobCount) {
        this.openJobCount = openJobCount;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }


    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "archived_by")
    private UUID archivedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "archive_reason")
    private ArchiveReason archiveReason;

    @Column(name = "archive_note", length = 500)
    private String archiveNote;

    @Column(name = "merged_into_id")
    private UUID mergedIntoId;

    @Column(name = "legal_name", length = 300)
    private String legalName;

    @Column(name = "tax_id", length = 20)
    private String taxId;

    @Column(name = "tax_office", length = 100)
    private String taxOffice;

    @Column(name = "mersis_no", length = 16)
    private String mersisNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "einvoice_status")
    private EInvoiceStatus einvoiceStatus = EInvoiceStatus.UNKNOWN;

    @Column(name = "einvoice_alias", length = 100)
    private String einvoiceAlias;

    @Column(name = "billing_address", length = 500)
    private String billingAddress;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "billing_emails", length = 254)
    private List<String> billingEmails = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "billing_currency", length = 3)
    private String billingCurrency;

    @Column(name = "vat_exempt")
    private boolean vatExempt;

    @Column(name = "withholding_applies")
    private boolean withholdingApplies;

    @Column(name = "parent_account_id")
    private UUID parentAccountId;

    @Column(name = "group_name", length = 200)
    private String groupName;

    @Enumerated(EnumType.STRING)
    @Column(name = "employee_band")
    private EmployeeBand employeeBand;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "hq_country", length = 2)
    private String hqCountry;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidentiality_level")
    private ConfidentialityLevel confidentialityLevel = ConfidentialityLevel.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_language")
    private Language preferredLanguage;

    @Column(name = "is_client")
    private boolean client;

    @Column(name = "is_prospect")
    private boolean prospect;

    @Column(name = "is_target")
    private boolean target;

    @Column(name = "is_employer")
    private boolean employer;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "aliases")
    private List<String> aliases = new ArrayList<>();

    @org.hibernate.annotations.Generated(event = {org.hibernate.generator.EventType.INSERT, org.hibernate.generator.EventType.UPDATE})
    @NotAudited
    @Column(name = "normalized_name", length = 200, insertable = false, updatable = false)
    private String normalizedName;

    public Instant getArchivedAt() {
        return archivedAt;
    }
    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public UUID getArchivedBy() {
        return archivedBy;
    }
    public void setArchivedBy(UUID archivedBy) {
        this.archivedBy = archivedBy;
    }

    public ArchiveReason getArchiveReason() {
        return archiveReason;
    }
    public void setArchiveReason(ArchiveReason archiveReason) {
        this.archiveReason = archiveReason;
    }

    public String getArchiveNote() {
        return archiveNote;
    }
    public void setArchiveNote(String archiveNote) {
        this.archiveNote = archiveNote;
    }

    public UUID getMergedIntoId() {
        return mergedIntoId;
    }
    public void setMergedIntoId(UUID mergedIntoId) {
        this.mergedIntoId = mergedIntoId;
    }

    public String getLegalName() {
        return legalName;
    }
    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getTaxId() {
        return taxId;
    }
    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getTaxOffice() {
        return taxOffice;
    }
    public void setTaxOffice(String taxOffice) {
        this.taxOffice = taxOffice;
    }

    public String getMersisNo() {
        return mersisNo;
    }
    public void setMersisNo(String mersisNo) {
        this.mersisNo = mersisNo;
    }

    public EInvoiceStatus getEinvoiceStatus() {
        return einvoiceStatus;
    }
    public void setEinvoiceStatus(EInvoiceStatus einvoiceStatus) {
        this.einvoiceStatus = einvoiceStatus;
    }

    public String getEinvoiceAlias() {
        return einvoiceAlias;
    }
    public void setEinvoiceAlias(String einvoiceAlias) {
        this.einvoiceAlias = einvoiceAlias;
    }

    public String getBillingAddress() {
        return billingAddress;
    }
    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public List<String> getBillingEmails() {
        return billingEmails;
    }
    public void setBillingEmails(List<String> billingEmails) {
        this.billingEmails = billingEmails;
    }

    public String getBillingCurrency() {
        return billingCurrency;
    }
    public void setBillingCurrency(String billingCurrency) {
        this.billingCurrency = billingCurrency;
    }

    public boolean isVatExempt() {
        return vatExempt;
    }
    public void setVatExempt(boolean vatExempt) {
        this.vatExempt = vatExempt;
    }

    public boolean isWithholdingApplies() {
        return withholdingApplies;
    }
    public void setWithholdingApplies(boolean withholdingApplies) {
        this.withholdingApplies = withholdingApplies;
    }

    public UUID getParentAccountId() {
        return parentAccountId;
    }
    public void setParentAccountId(UUID parentAccountId) {
        this.parentAccountId = parentAccountId;
    }

    public String getGroupName() {
        return groupName;
    }
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public EmployeeBand getEmployeeBand() {
        return employeeBand;
    }
    public void setEmployeeBand(EmployeeBand employeeBand) {
        this.employeeBand = employeeBand;
    }

    public String getHqCountry() {
        return hqCountry;
    }
    public void setHqCountry(String hqCountry) {
        this.hqCountry = hqCountry;
    }

    public ConfidentialityLevel getConfidentialityLevel() {
        return confidentialityLevel;
    }
    public void setConfidentialityLevel(ConfidentialityLevel confidentialityLevel) {
        this.confidentialityLevel = confidentialityLevel;
    }

    public Language getPreferredLanguage() {
        return preferredLanguage;
    }
    public void setPreferredLanguage(Language preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public boolean isClient() {
        return client;
    }
    public void setClient(boolean client) {
        this.client = client;
    }

    public boolean isProspect() {
        return prospect;
    }
    public void setProspect(boolean prospect) {
        this.prospect = prospect;
    }

    public boolean isTarget() {
        return target;
    }
    public void setTarget(boolean target) {
        this.target = target;
    }

    public boolean isEmployer() {
        return employer;
    }
    public void setEmployer(boolean employer) {
        this.employer = employer;
    }

    public List<String> getAliases() {
        return aliases;
    }
    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    /** Satisfies the role CHECK for accounts created without a role: prospect only when no flag is set (R63). */
    @PrePersist
    void defaultRole() {
        if (!(client || prospect || target || employer)) {
            prospect = true;
        }
    }
}
