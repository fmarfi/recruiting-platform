package com.recruiting.platform.security.user.app;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.security.CurrentUser;
import com.recruiting.platform.security.domain.AppUser;
import com.recruiting.platform.security.domain.AppUserRepository;
import com.recruiting.platform.security.user.web.dto.UserCreateRequest;
import com.recruiting.platform.security.user.web.dto.UserUpdateRequest;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Users: list, create, change, reset password. Users are never deleted, only deactivated. */
@Service
public class UserService {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final CurrentUser currentUser;

    UserService(AppUserRepository users, PasswordEncoder encoder, CurrentUser currentUser) {
        this.users = users;
        this.encoder = encoder;
        this.currentUser = currentUser;
    }

    /** Everyone may list (owner pickers); only admins see inactive users, and only if they ask. */
    @Transactional(readOnly = true)
    public Page<AppUser> list(String q, Role role, Boolean active, Pageable pageable) {
        Boolean effectiveActive = currentUser.hasAtLeast(Role.ADMIN) ? active : Boolean.TRUE;
        return users.findAll(matching(q, role, effectiveActive), pageable);
    }

    @Transactional(readOnly = true)
    public AppUser get(UUID id) {
        return users.findById(id).orElseThrow(() -> notFound(id));
    }

    @Transactional
    public AppUser create(UserCreateRequest request) {
        String email = request.email().trim();
        users.findByEmailIgnoreCase(email).ifPresent(existing -> {
            throw new ApiException(ErrorCode.DUPLICATE, "A user with this e-mail already exists.",
                    Map.of("existingId", existing.getId().toString()));
        });
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setName(request.name().trim());
        user.setRole(request.role());
        user.setLanguage(request.language() == null ? Language.TR : request.language());
        user.setPasswordHash(encoder.encode(request.initialPassword()));
        user.setMustChangePassword(true);
        return users.saveAndFlush(user);
    }

    @Transactional
    public AppUser update(UUID id, UserUpdateRequest request) {
        AppUser user = get(id);
        if (request.version() != null && !request.version().equals(user.getVersion())) {
            throw new ApiException(ErrorCode.VERSION_CONFLICT, "The user was changed by someone else.");
        }
        boolean self = currentUser.idOrEmpty().filter(id::equals).isPresent();
        boolean losesAdmin = Boolean.FALSE.equals(request.active())
                || (request.role() != null && request.role() != Role.ADMIN);
        if (self && losesAdmin) {
            throw new ApiException(ErrorCode.STATE_CONFLICT,
                    "You cannot deactivate yourself or lower your own role.");
        }
        if (request.name() != null) {
            user.setName(request.name().trim());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
        if (request.language() != null) {
            user.setLanguage(request.language());
        }
        return users.saveAndFlush(user);
    }

    @Transactional
    public void resetPassword(UUID id, String initialPassword) {
        AppUser user = get(id);
        user.setPasswordHash(encoder.encode(initialPassword));
        user.setMustChangePassword(true);
        users.save(user);
    }

    private static ApiException notFound(UUID id) {
        return new ApiException(ErrorCode.NOT_FOUND, "User " + id + " does not exist.");
    }

    private static Specification<AppUser> matching(String q, Role role, Boolean active) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase().replace("%", "") + "%";
                predicate = cb.and(predicate, cb.or(cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            if (role != null) {
                predicate = cb.and(predicate, cb.equal(root.get("role"), role));
            }
            if (active != null) {
                predicate = cb.and(predicate, cb.equal(root.get("active"), active));
            }
            return predicate;
        };
    }
}
