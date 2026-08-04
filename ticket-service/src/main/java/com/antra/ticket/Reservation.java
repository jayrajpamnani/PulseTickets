package com.antra.ticket;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reservations")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Reservation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @NotBlank
  @Column(nullable = false)
  public String username;

  @NotNull
  @Column(nullable = false)
  public Long eventId;

  @Min(1)
  @Column(nullable = false)
  public int quantity;

  @NotNull
  @DecimalMin("0.01")
  @Column(nullable = false)
  public BigDecimal totalPrice;

  @NotBlank
  @Column(nullable = false)
  public String status = "PENDING";

  @Column(nullable = false)
  public Instant createdAt = Instant.now();

  protected Reservation() {}

  public Reservation(String user, Long event, int count, BigDecimal price) {
    if (user == null || user.isBlank()) throw new IllegalArgumentException("Username required");
    if (event == null) throw new IllegalArgumentException("Event ID required");
    if (count <= 0) throw new IllegalArgumentException("Quantity must be at least 1");
    if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Price must be positive");

    this.username = user;
    this.eventId = event;
    this.quantity = count;
    this.totalPrice = price.multiply(BigDecimal.valueOf(count));
  }

  @PrePersist
  @PreUpdate
  public void validateAmounts() {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be positive");
    }
    if (totalPrice != null && totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Total price must be positive");
    }
  }
}
