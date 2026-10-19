package com.recruiting.platform.security.user.web;

import com.recruiting.platform.contracts.Role;
import com.recruiting.platform.security.ApiPage;
import com.recruiting.platform.security.ApiPageable;
import com.recruiting.platform.security.user.app.UserService;
import com.recruiting.platform.security.user.web.dto.PasswordResetRequest;
import com.recruiting.platform.security.user.web.dto.UserCreateRequest;
import com.recruiting.platform.security.user.web.dto.UserResponse;
import com.recruiting.platform.security.user.web.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tag Users. */
@RestController
@RequestMapping("/api/v1/users")
class UserController {

    private static final Map<String, String> SORTABLE = Map.of("name", "name", "email", "email", "role", "role",
            "createdAt", "createdAt", "lastLoginAt", "lastLoginAt");

    private final UserService users;

    UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    @PreAuthorize("hasRole('CONSULTANT')")
    ApiPage<UserResponse> list(@RequestParam(required = false) String q, @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size, @RequestParam(required = false) List<String> sort) {
        var pageable = ApiPageable.of(page, size, sort, SORTABLE, Sort.by("name"));
        return ApiPage.of(users.list(q, role, active, pageable), UserResponse::from);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest body) {
        UserResponse created = UserResponse.from(users.create(body));
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CONSULTANT')")
    UserResponse get(@PathVariable UUID id) {
        return UserResponse.from(users.get(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    UserResponse update(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequest body) {
        return UserResponse.from(users.update(id, body));
    }

    @PostMapping("/{id}/password-reset")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetPassword(@PathVariable UUID id, @Valid @RequestBody PasswordResetRequest body) {
        users.resetPassword(id, body.initialPassword());
    }
}
