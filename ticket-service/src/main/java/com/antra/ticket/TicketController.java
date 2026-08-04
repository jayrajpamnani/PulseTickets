package com.antra.ticket;

import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/reservations")
public class TicketController {
  private final TicketService ticketService;

  public TicketController(TicketService ticketService) {
    this.ticketService = ticketService;
  }

  @PostMapping
  public ResponseEntity<Reservation> reserve(
      Authentication auth,
      @RequestHeader("Authorization") String bearer,
      @RequestParam Long eventId,
      @RequestParam @Min(1) int quantity) {
    if (auth == null || auth.getName() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
    }
    Reservation saved = ticketService.reserve(auth.getName(), bearer, eventId, quantity);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
  }

  @GetMapping
  public List<Reservation> mine(Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
    }
    return ticketService.mine(auth.getName());
  }

  @GetMapping("/{id}")
  public Reservation one(@PathVariable Long id, Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
    }
    return ticketService.one(id, auth.getName());
  }

  @PutMapping("/{id}/cancel")
  public Reservation cancel(
      @PathVariable Long id,
      Authentication auth,
      @RequestHeader("Authorization") String bearer) {
    if (auth == null || auth.getName() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
    }
    return ticketService.cancel(id, auth.getName(), bearer);
  }
}
