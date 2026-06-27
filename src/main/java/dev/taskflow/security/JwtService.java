package dev.taskflow.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtService {

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(@Value("${taskflow.jwt.secret}") String secret,
                      @Value("${taskflow.jwt.ttl-seconds}") long ttlSeconds) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            // pad to 32 bytes to satisfy HS256 minimum
            byte[] padded = new byte[32];
            System.arraycopy(raw, 0, padded, 0, Math.min(raw.length, 32));
            raw = padded;
        }
        this.key = Keys.hmacShaKeyFor(raw);
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(UUID userId, String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        var jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        var body = jws.getPayload();
        return new Claims(
                UUID.fromString(body.getSubject()),
                body.get("username", String.class),
                body.get("role", String.class)
        );
    }

    public record Claims(UUID userId, String username, String role) {}
}
