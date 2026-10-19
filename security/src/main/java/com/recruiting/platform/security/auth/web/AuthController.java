package com.recruiting.platform.security.auth.web;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.security.auth.app.AuthService;
import com.recruiting.platform.security.auth.web.dto.CurrentUserResponse;
import com.recruiting.platform.security.auth.web.dto.CurrentUserUpdateRequest;
import com.recruiting.platform.security.auth.web.dto.LoginRequest;
import com.recruiting.platform.security.auth.web.dto.PasswordChangeRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tag Auth: login, logout, own profile and password. */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final AuthService auth;

    AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    CurrentUserResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
            HttpServletResponse response) {
        return auth.login(body.email(), body.password(), request, response);
    }

    @PostMapping("/logout")
    @PreAuthorize("hasRole('CONSULTANT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request) {
        auth.logout(request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CONSULTANT')")
    CurrentUserResponse me() {
        return auth.me();
    }

    @PatchMapping("/me")
    @PreAuthorize("hasRole('CONSULTANT')")
    CurrentUserResponse updateMe(@Valid @RequestBody CurrentUserUpdateRequest body) {
        if (body.name() == null && body.language() == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Send a name or a language.");
        }
        return auth.updateMe(body.name(), body.language());
    }

    @PostMapping("/password")
    @PreAuthorize("hasRole('CONSULTANT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@Valid @RequestBody PasswordChangeRequest body) {
        auth.changePassword(body.currentPassword(), body.newPassword());
    }
}
