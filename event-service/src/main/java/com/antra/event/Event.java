package com.antra.event;

import com.antra.event.dto.CreateEventDTO;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "events")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Event {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @NotBlank
  @Column(nullable = false)
  public String title;

  @NotBlank
  @Column(nullable = false)
  public String venue;

  @NotNull
  @Column(nullable = false)
  public Instant startsAt;

  @NotNull
  @DecimalMin("0.01")
  @Column(nullable = false)
  public BigDecimal price;

  @Min(1)
  @Column(nullable = false)
  public int capacity;

  @Min(0)
  @Column(nullable = false)
  public int availableTickets;

  @Column(nullable = false, length = 2000)
  public String description = "";

  public String bannerUrl;

  @Version
  public Long version;

  protected Event() {}

  public Event(CreateEventDTO c) {
    if (c.price() == null || c.price().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Price must be greater than zero");
    }
    if (c.capacity() <= 0) {
      throw new IllegalArgumentException("Capacity must be positive");
    }
    this.title = c.title();
    this.venue = c.venue();
    this.startsAt = c.startsAt();
    this.price = c.price();
    this.capacity = c.capacity();
    this.description = c.description() == null ? "" : c.description();
    this.bannerUrl = c.bannerUrl();
    this.availableTickets = c.capacity();
  }

  @PrePersist
  @PreUpdate
  public void validateAmounts() {
    if (price != null && price.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Price must be positive");
    }
    if (capacity <= 0) {
      throw new IllegalArgumentException("Capacity must be positive");
    }
    if (availableTickets < 0) {
      throw new IllegalArgumentException("Available tickets cannot be negative");
    }
  }
}
