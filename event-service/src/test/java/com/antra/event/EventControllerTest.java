package com.antra.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class EventControllerTest {
  @Test void deleteMissingEventReturnsNotFound() {
    EventService service = mock(EventService.class);
    when(service.exists(99L)).thenReturn(false);
    EventController controller = new EventController(service);
    try { controller.delete(99L); throw new AssertionError("expected not found"); }
    catch (ResponseStatusException ex) { assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode()); }
    verify(service, never()).delete(99L);
  }
}
