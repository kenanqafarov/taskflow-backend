package dev.taskflow.api;

import dev.taskflow.api.Dtos.*;
import dev.taskflow.domain.UserEntity;
import dev.taskflow.repo.UserRepository;
import dev.taskflow.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserController(UserRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }

    @GetMapping
    public List<UserDto> list() {
        CurrentUser.require();
        return users.findAll().stream().map(UserDto::of).toList();
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable UUID id) {
        CurrentUser.require();
        return users.findById(id).map(UserDto::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @PostMapping
    public UserDto create(@RequestBody CreateUserReq req) {
        CurrentUser.requireAdmin();
        if (req.username() == null || req.password() == null
                || req.firstName() == null || req.lastName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username, password, firstName, lastName are required");
        }
        String username = req.username().trim();
        if (username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }
        if (req.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (users.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        UserEntity.Role role = UserEntity.Role.MEMBER;
        if (req.role() != null) {
            try { role = UserEntity.Role.valueOf(req.role()); } catch (Exception ignored) {}
            if (role == UserEntity.Role.SUPER_ADMIN) role = UserEntity.Role.ADMIN; // never create another super
        }
        UserEntity u = UserEntity.builder()
                .username(username)
                .passwordHash(encoder.encode(req.password()))
                .firstName(req.firstName())
                .lastName(req.lastName())
                .email(req.email())
                .githubUrl(req.githubUrl())
                .linkedinUrl(req.linkedinUrl())
                .avatarUrl(req.avatarUrl())
                .role(role)
                .active(true)
                .build();
        return UserDto.of(users.save(u));
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable UUID id, @RequestBody UpdateUserReq req) {
        CurrentUser.requireAdmin();
        UserEntity u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (req.username() != null && !req.username().isBlank() && !req.username().equals(u.getUsername())) {
            if (users.existsByUsername(req.username())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
            }
            u.setUsername(req.username().trim());
        }
        if (req.password() != null && !req.password().isBlank()) u.setPasswordHash(encoder.encode(req.password()));
        if (req.firstName() != null && !req.firstName().isBlank()) u.setFirstName(req.firstName().trim());
        if (req.lastName() != null && !req.lastName().isBlank()) u.setLastName(req.lastName().trim());
        u.setEmail(req.email());
        u.setGithubUrl(req.githubUrl());
        u.setLinkedinUrl(req.linkedinUrl());
        u.setAvatarUrl(req.avatarUrl());
        if (req.role() != null && u.getRole() != UserEntity.Role.SUPER_ADMIN) {
            try {
                UserEntity.Role role = UserEntity.Role.valueOf(req.role());
                if (role != UserEntity.Role.SUPER_ADMIN) u.setRole(role);
            } catch (IllegalArgumentException ignored) {}
        }
        if (req.active() != null && u.getRole() != UserEntity.Role.SUPER_ADMIN) {
            u.setActive(req.active());
        }
        return UserDto.of(users.save(u));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        CurrentUser.requireAdmin();
        users.findById(id).ifPresent(u -> {
            if (u.getRole() == UserEntity.Role.SUPER_ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot remove super admin");
            }
            users.delete(u);
        });
    }
}
