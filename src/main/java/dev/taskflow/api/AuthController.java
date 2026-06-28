package dev.taskflow.api;

import dev.taskflow.api.Dtos.*;
import dev.taskflow.domain.UserEntity;
import dev.taskflow.repo.UserRepository;
import dev.taskflow.security.CurrentUser;
import dev.taskflow.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReq req) {
        String usernameOrEmail = req.username() == null ? "" : req.username().trim();
        String password = req.password() == null ? "" : req.password();
        UserEntity u = users.findFirstByUsernameIgnoreCaseOrEmailIgnoreCase(usernameOrEmail, usernameOrEmail)
                .orElse(null);
        if (u == null || !u.isActive() || !encoder.matches(password, u.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Username or password is incorrect"));
        }
        String token = jwt.issue(u.getId(), u.getUsername(), u.getRole().name());
        return ResponseEntity.ok(new LoginRes(token, UserDto.of(u)));
    }

    @GetMapping("/me")
    public UserDto me() {
        var p = CurrentUser.require();
        return users.findById(p.id()).map(UserDto::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/me")
    public UserDto updateMe(@RequestBody UpdateProfileReq req) {
        var principal = CurrentUser.require();
        UserEntity user = users.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (req.username() != null && !req.username().isBlank()
                && !req.username().trim().equalsIgnoreCase(user.getUsername())) {
            String username = req.username().trim();
            if (users.existsByUsername(username)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
            }
            user.setUsername(username);
        }
        if (req.firstName() != null && !req.firstName().isBlank()) user.setFirstName(req.firstName().trim());
        if (req.lastName() != null && !req.lastName().isBlank()) user.setLastName(req.lastName().trim());
        if (req.email() != null) user.setEmail(req.email().trim());
        if (req.githubUrl() != null) user.setGithubUrl(req.githubUrl().trim());
        if (req.linkedinUrl() != null) user.setLinkedinUrl(req.linkedinUrl().trim());
        if (req.avatarUrl() != null) user.setAvatarUrl(req.avatarUrl().trim());
        if (req.notificationsEnabled() != null) user.setNotificationsEnabled(req.notificationsEnabled());
        if (req.theme() != null && ("dark".equals(req.theme()) || "light".equals(req.theme()))) {
            user.setTheme(req.theme());
        }
        if (req.newPassword() != null && !req.newPassword().isBlank()) {
            if (req.currentPassword() == null || !encoder.matches(req.currentPassword(), user.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
            }
            user.setPasswordHash(encoder.encode(req.newPassword()));
        }
        return UserDto.of(users.save(user));
    }
}
