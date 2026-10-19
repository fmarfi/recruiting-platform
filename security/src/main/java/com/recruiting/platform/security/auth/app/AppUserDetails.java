package com.recruiting.platform.security.auth.app;

import com.recruiting.platform.security.IdentifiedUser;
import com.recruiting.platform.security.domain.AppUser;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** The session principal: carries our user id (see {@link IdentifiedUser}) and the single ROLE_ authority. */
public class AppUserDetails implements UserDetails, IdentifiedUser, Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final String role;

    public AppUserDetails(AppUser user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.active = user.isActive();
        this.role = user.getRole().name();
    }

    @Override
    public UUID userId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
