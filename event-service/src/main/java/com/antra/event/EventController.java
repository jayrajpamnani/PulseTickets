package com.antra.event;

import com.antra.event.dto.CreateEventDTO;
import com.antra.event.dto.ReservationResponseDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/events")
public class EventController {
  private final EventService events;

  public EventController(EventService events) {
    this.events = events;
  }

  @GetMapping
  public Page<Event> list(
      @RequestParam(name = "keyword", defaultValue = "") String keyword,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    if (page < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page number cannot be negative");
    }
    if (size <= 0 || size > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and 100");
    }
    return events.search(keyword.trim(), PageRequest.of(page, size, Sort.by("startsAt")));
  }

  @GetMapping("/{id}")
  public Event one(@PathVariable("id") Long id) {
    return events.find(id);
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Event> create(@Valid @RequestBody CreateEventDTO c) {
    return ResponseEntity.status(HttpStatus.CREATED).body(events.save(new Event(c)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public Event update(@PathVariable("id") Long id, @Valid @RequestBody CreateEventDTO c) {
    Event e = one(id);
    e.title = c.title();
    e.venue = c.venue();
    e.startsAt = c.startsAt();
    e.price = c.price();
    e.description = c.description() == null ? "" : c.description();
    e.bannerUrl = c.bannerUrl();
    return events.save(e);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
    if (!events.exists(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    events.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/reserve")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  @Transactional
  public ReservationResponseDTO reserve(@PathVariable("id") Long id, @RequestParam(name = "quantity") @Min(1) int quantity) {
    Event e = one(id);
    if (e.availableTickets < quantity) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient tickets");
    }
    e.availableTickets -= quantity;
    return new ReservationResponseDTO(e.id, e.title, e.price, quantity);
  }

  @PostMapping("/{id}/release")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  @Transactional
  public void release(@PathVariable("id") Long id, @RequestParam(name = "quantity") @Min(1) int quantity) {
    Event e = one(id);
    e.availableTickets = Math.min(e.capacity, e.availableTickets + quantity);
  }
}
