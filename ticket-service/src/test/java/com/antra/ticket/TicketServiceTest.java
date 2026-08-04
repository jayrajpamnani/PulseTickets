package com.antra.ticket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;

class TicketServiceTest {
  private ReservationRepository repo;
  private EventClient events;
  private KafkaTemplate<String, Object> kafka;
  private TicketService service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    repo = mock(ReservationRepository.class);
    events = mock(EventClient.class);
    kafka = mock(KafkaTemplate.class);
    service = new TicketService(repo, kafka, events);
  }

  @Test
  void reserveSuccess() {
    doReturn(Map.of("unitPrice", "25.00")).when(events).reserve(1L, 2, "Bearer token");
    when(repo.save(any(Reservation.class))).thenAnswer(i -> {
      Reservation r = i.getArgument(0);
      r.id = 10L;
      return r;
    });

    Reservation r = service.reserve("alice", "Bearer token", 1L, 2);
    assertNotNull(r);
    assertEquals("alice", r.username);
    assertEquals(new BigDecimal("50.00"), r.totalPrice);
    verify(kafka).send(eq("reservation-created"), eq("10"), any());
  }

  @Test
  void reserveInvalidQuantityThrowsBadRequest() {
    var ex = assertThrows(ResponseStatusException.class, () -> service.reserve("alice", "token", 1L, 0));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void oneReturnsForbiddenForOtherUser() {
    Reservation r = new Reservation("bob", 1L, 1, new BigDecimal("10.00"));
    when(repo.findById(5L)).thenReturn(Optional.of(r));

    var ex = assertThrows(ResponseStatusException.class, () -> service.one(5L, "alice"));
    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
  }

  @Test
  void cancelPaidReservationThrowsConflict() {
    Reservation r = new Reservation("alice", 1L, 1, new BigDecimal("10.00"));
    r.status = "PAID";
    when(repo.findById(5L)).thenReturn(Optional.of(r));

    var ex = assertThrows(ResponseStatusException.class, () -> service.cancel(5L, "alice", "token"));
    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
  }
}
