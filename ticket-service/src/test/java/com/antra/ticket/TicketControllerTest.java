package com.antra.ticket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class TicketControllerTest {
  @Test
  void reserveWithoutAuthThrowsUnauthorized() {
    TicketService service = mock(TicketService.class);
    TicketController controller = new TicketController(service);

    assertThrows(ResponseStatusException.class, () -> controller.reserve(null, "Bearer token", 1L, 2));
  }

  @Test
  void mineWithoutAuthThrowsUnauthorized() {
    TicketService service = mock(TicketService.class);
    TicketController controller = new TicketController(service);

    assertThrows(ResponseStatusException.class, () -> controller.mine(null));
  }
}
