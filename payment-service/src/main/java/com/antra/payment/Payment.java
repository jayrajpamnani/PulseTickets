package com.antra.payment;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Payment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @NotNull
  @Column(unique = true, nullable = false)
  public Long reservationId;

  @NotBlank
  @Column(nullable = false)
  public String username;

  @NotNull
  @DecimalMin("0.01")
  @Column(nullable = false)
  public BigDecimal amount;

  @NotBlank
  @Column(nullable = false)
  public String status;

  public Instant paidAt;

  protected Payment() {}

  public Payment(Long r, String u, BigDecimal a) {
    if (r == null) throw new IllegalArgumentException("Reservation ID required");
    if (u == null || u.isBlank()) throw new IllegalArgumentException("Username required");
    if (a == null || a.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be positive");

    this.reservationId = r;
    this.username = u;
    this.amount = a;
    this.status = "SUCCESS";
    this.paidAt = Instant.now();
  }

  @PrePersist
  @PreUpdate
  public void validateAmounts() {
    if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Payment amount must be positive");
    }
  }
}
