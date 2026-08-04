package com.antra.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class EventControllerTest {
  @Test
  void deleteMissingEventReturnsNotFound() {
    EventService service = mock(EventService.class);
    when(service.exists(99L)).thenReturn(false);
    EventController controller = new EventController(service);

    var ex = assertThrows(ResponseStatusException.class, () -> controller.delete(99L));
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    verify(service, never()).delete(99L);
  }

  @Test
  void invalidPageNumberThrowsBadRequest() {
    EventService service = mock(EventService.class);
    EventController controller = new EventController(service);

    var ex = assertThrows(ResponseStatusException.class, () -> controller.list("", -1, 20));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void invalidPageSizeThrowsBadRequest() {
    EventService service = mock(EventService.class);
    EventController controller = new EventController(service);

    var ex = assertThrows(ResponseStatusException.class, () -> controller.list("", 0, 0));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }
}
