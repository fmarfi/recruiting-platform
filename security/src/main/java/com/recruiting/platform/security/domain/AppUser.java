package com.recruiting.platform.security.domain;

import jakarta.persistence.*;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.Role;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/** Maps table {@code app_user}. Other modules reference it by id only (ADR 0001). */
@Entity
@Table(name = "app_user")
@Audited
public class AppUser extends AuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "name", length = 120)
    private String name;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "language")
    @Enumerated(EnumType.STRING)
    private Language language = Language.TR;

    @NotAudited
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "must_change_password")
    private boolean mustChangePassword = true;

    @NotAudited
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    public AppUser() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

}
