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
        if (u == null || !encoder.matches(password, u.getPasswordHash())) {
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
}
