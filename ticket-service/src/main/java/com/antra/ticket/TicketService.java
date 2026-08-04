package com.antra.ticket;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketService {
  private final ReservationRepository repo;
  private final EventClient events;
  private final KafkaTemplate<String, Object> kafka;

  public TicketService(ReservationRepository repo, KafkaTemplate<String, Object> kafka, EventClient events) {
    this.repo = repo;
    this.kafka = kafka;
    this.events = events;
  }

  @Transactional
  public Reservation reserve(String username, String bearer, Long eventId, int quantity) {
    if (quantity <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
    }
    Map<?, ?> event = events.reserve(eventId, quantity, bearer);
    BigDecimal price = new BigDecimal(event.get("unitPrice").toString());
    Reservation saved = repo.save(new Reservation(username, eventId, quantity, price));
    kafka.send("reservation-created", saved.id.toString(), Map.of(
        "reservationId", saved.id,
        "username", saved.username,
        "eventId", eventId,
        "total", saved.totalPrice
    ));
    return saved;
  }

  public List<Reservation> mine(String username) {
    return repo.findByUsernameOrderByCreatedAtDesc(username);
  }

  public Reservation one(Long id, String username) {
    Reservation r = repo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
    if (!r.username.equals(username)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
    return r;
  }

  @Transactional
  public Reservation cancel(Long id, String username, String bearer) {
    Reservation r = one(id, username);
    if ("PAID".equals(r.status)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel a paid reservation");
    }
    r.status = "CANCELLED";
    events.release(r.eventId, r.quantity, bearer);
    return repo.save(r);
  }
}
