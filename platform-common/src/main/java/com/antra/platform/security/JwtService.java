package com.antra.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;
  public JwtService(@Value("${app.jwt.secret:change-me-change-me-change-me-change-me}") String secret) { key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); }
  public String issue(String username, String role) { Instant now = Instant.now(); return Jwts.builder().subject(username).claim("role", role).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(3600))).signWith(key).compact(); }
  public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}
