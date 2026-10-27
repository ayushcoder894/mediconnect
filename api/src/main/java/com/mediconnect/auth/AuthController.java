package com.mediconnect.auth;

import com.mediconnect.audit.AuditService;
import com.mediconnect.common.AppProperties;
import com.mediconnect.security.CurrentUser;
import com.mediconnect.security.JwtAuthFilter;
import com.mediconnect.security.JwtService;
import com.mediconnect.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final AppProperties props;
    private final AuditService audit;

    @PostMapping("/register/patient")
    public ResponseEntity<AuthService.MeDto> registerPatient(@Valid @RequestBody AuthService.RegisterPatientRequest req) {
        return withSession(authService.registerPatient(req), HttpStatus.CREATED);
    }

    @PostMapping("/register/doctor")
    public ResponseEntity<AuthService.MeDto> registerDoctor(@Valid @RequestBody AuthService.RegisterDoctorRequest req) {
        return withSession(authService.registerDoctor(req), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthService.MeDto> login(@Valid @RequestBody AuthService.LoginRequest req) {
        User user = authService.login(req);
        audit.record("LOGIN", "User", user.getId(), user.getRole().name());
        return withSession(user, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    @GetMapping("/me")
    public AuthService.MeDto me() {
        return authService.me(CurrentUser.get());
    }

    private ResponseEntity<AuthService.MeDto> withSession(User user, HttpStatus status) {
        String token = jwtService.issue(user);
        AuthService.MeDto me = authService.toMe(user);
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie(token, jwtService.ttl()).toString())
                // Cross-site hosting (site and API on different domains): browsers such as
                // Safari drop third-party cookies, so the client also gets the token to send
                // as a bearer header. Same-origin deployments keep it cookie-only.
                .body(props.jwt().crossSite() ? me.withToken(token) : me);
    }

    /**
     * httpOnly so scripts cannot read the token. SameSite=Strict on a single origin;
     * cross-site hosting needs SameSite=None, which browsers only accept with Secure.
     */
    private ResponseCookie cookie(String value, Duration maxAge) {
        boolean crossSite = props.jwt().crossSite();
        return ResponseCookie.from(JwtAuthFilter.COOKIE_NAME, value)
                .httpOnly(true)
                .secure(crossSite || props.jwt().secureCookie())
                .sameSite(crossSite ? "None" : "Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
