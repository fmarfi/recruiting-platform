package com.recruiting.platform.security.auth.app;

import com.recruiting.platform.contracts.AccessAction;
import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.EntityType;
import com.recruiting.platform.contracts.Language;
import com.recruiting.platform.contracts.ErrorCode;
import com.recruiting.platform.security.AccessRecorder;
import com.recruiting.platform.security.CurrentUser;
import com.recruiting.platform.security.RequestRateLimiter;
import com.recruiting.platform.security.TooManyRequestsException;
import com.recruiting.platform.security.auth.web.dto.CurrentUserResponse;
import com.recruiting.platform.security.domain.AppUser;
import com.recruiting.platform.security.domain.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Login, logout and the logged-in user's own data (S-01 of the security stories, NFR-05). */
@Service
public class AuthService {

    private final AuthenticationManager authentication;
    private final SecurityContextRepository contexts;
    private final LoginAttemptTracker attempts;
    private final RequestRateLimiter rateLimiter;
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final AccessRecorder accessRecorder;
    private final CurrentUser currentUser;
    private final int loginsPerMinutePerIp;

    AuthService(AuthenticationManager authentication, SecurityContextRepository contexts,
            LoginAttemptTracker attempts, RequestRateLimiter rateLimiter, AppUserRepository users,
            PasswordEncoder encoder, AccessRecorder accessRecorder, CurrentUser currentUser,
            @org.springframework.beans.factory.annotation.Value("${app.security.login-rate-per-minute:30}") int loginsPerMinutePerIp) {
        this.loginsPerMinutePerIp = loginsPerMinutePerIp;
        this.authentication = authentication;
        this.contexts = contexts;
        this.attempts = attempts;
        this.rateLimiter = rateLimiter;
        this.users = users;
        this.encoder = encoder;
        this.accessRecorder = accessRecorder;
        this.currentUser = currentUser;
    }

    /** Checks the password, starts the session, stamps the last login and writes the access-log row. */
    @Transactional
    public CurrentUserResponse login(String email, String password, HttpServletRequest request,
            HttpServletResponse response) {
        if (!rateLimiter.tryAcquire("login:" + request.getRemoteAddr(), loginsPerMinutePerIp,
                Duration.ofMinutes(1))) {
            throw new TooManyRequestsException();
        }
        if (attempts.isLocked(email)) {
            throw badCredentials();
        }
        Authentication result;
        try {
            result = authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        } catch (AuthenticationException ex) {
            attempts.failed(email);
            throw badCredentials();
        }
        attempts.succeeded(email);
        AppUserDetails principal = (AppUserDetails) result.getPrincipal();
        request.getSession(true);
        request.changeSessionId(); // session fixation protection
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null,
                principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        users.markLoggedIn(principal.userId(), Instant.now());
        accessRecorder.recordFor(principal.userId(), AccessAction.VIEW, EntityType.USER, principal.userId(),
                request.getRemoteAddr());
        return CurrentUserResponse.from(load(principal.userId()));
    }

    public void logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse me() {
        return CurrentUserResponse.from(load(currentUser.id()));
    }

    @Transactional
    public CurrentUserResponse updateMe(String name, Language language) {
        AppUser user = load(currentUser.id());
        if (name != null) {
            user.setName(name.trim());
        }
        if (language != null) {
            user.setLanguage(language);
        }
        return CurrentUserResponse.from(users.save(user));
    }

    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        AppUser user = load(currentUser.id());
        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "The current password is not correct.",
                    Map.of("errors", List.of(Map.of("field", "currentPassword", "code", "INCORRECT",
                            "message", "The current password is not correct."))));
        }
        user.setPasswordHash(encoder.encode(newPassword));
        user.setMustChangePassword(false);
        users.save(user);
    }

    private AppUser load(UUID id) {
        return users.findById(id).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED, "User not found."));
    }

    private static ApiException badCredentials() {
        return new ApiException(ErrorCode.BAD_CREDENTIALS,
                "E-mail or password is wrong, or the account is locked for a while.");
    }
}
