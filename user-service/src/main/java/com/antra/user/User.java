package com.antra.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @NotBlank
  @Column(unique = true, nullable = false)
  public String username;

  @NotBlank
  @Email
  @Column(unique = true, nullable = false)
  public String email;

  @NotBlank
  @Column(name = "password_hash", nullable = false)
  public String passwordHash;

  @NotBlank
  @Column(nullable = false)
  public String role = "USER";

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();

  protected User() {}

  public User(String u, String e, String p) {
    if (u == null || u.isBlank()) throw new IllegalArgumentException("Username cannot be blank");
    if (e == null || e.isBlank()) throw new IllegalArgumentException("Email cannot be blank");
    if (p == null || p.isBlank()) throw new IllegalArgumentException("Password hash cannot be blank");
    this.username = u;
    this.email = e;
    this.passwordHash = p;
  }
}
