package cn.edu.jnu.labflowreport.auth.security;

import cn.edu.jnu.labflowreport.auth.model.TokenPayload;
import cn.edu.jnu.labflowreport.config.SecurityJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expireMinutes;

    public JwtService(SecurityJwtProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expireMinutes = properties.getExpireMinutes();
    }

    public String createToken(Long userId, String username, List<String> roleCodes) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expireMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("roles", roleCodes)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public Instant expiresAt() {
        return Instant.now().plus(expireMinutes, ChronoUnit.MINUTES);
    }

    public TokenPayload parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long userId = claims.get("uid", Long.class);
        String username = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> roleCodes = (List<String>) claims.get("roles", List.class);
        return new TokenPayload(userId, username, roleCodes);
    }
}

