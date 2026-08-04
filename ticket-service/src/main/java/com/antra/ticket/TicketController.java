package com.antra.ticket;

import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/reservations")
class TicketController {
  private final ReservationRepository repo;
  private final EventClient events;
  private final KafkaTemplate<String, Object> kafka;

  TicketController(ReservationRepository repo, KafkaTemplate<String, Object> kafka, EventClient events) {
    this.repo = repo; this.kafka = kafka; this.events = events;
  }

  @PostMapping
  @Transactional
  ResponseEntity<Reservation> reserve(Authentication auth, @RequestHeader("Authorization") String bearer,
      @RequestParam Long eventId, @RequestParam @Min(1) int quantity) {
    Map<?, ?> event = events.reserve(eventId, quantity, bearer);
    BigDecimal price = new BigDecimal(event.get("unitPrice").toString());
    Reservation saved = repo.save(new Reservation(auth.getName(), eventId, quantity, price));
    kafka.send("reservation-created", saved.id.toString(), Map.of("reservationId", saved.id,
        "username", saved.username, "eventId", eventId, "total", saved.totalPrice));
    return ResponseEntity.status(201).body(saved);
  }

  @GetMapping List<Reservation> mine(Authentication auth) {
    return repo.findByUsernameOrderByCreatedAtDesc(auth.getName());
  }

  @GetMapping("/{id}") Reservation one(@PathVariable Long id, Authentication auth) {
    Reservation r = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!r.username.equals(auth.getName())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return r;
  }

  @PutMapping("/{id}/cancel")
  @Transactional
  Reservation cancel(@PathVariable Long id, Authentication auth, @RequestHeader("Authorization") String bearer) {
    Reservation r = one(id, auth);
    if ("PAID".equals(r.status)) throw new ResponseStatusException(HttpStatus.CONFLICT);
    r.status = "CANCELLED";
    events.release(r.eventId, r.quantity, bearer);
    return r;
  }
}
