package com.antra.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class EventServiceTest {
  @Test
  void findExistingReturnsEvent() {
    EventRepository repo = mock(EventRepository.class);
    Event event = mock(Event.class);
    when(repo.findById(10L)).thenReturn(Optional.of(event));

    EventService service = new EventService(repo);
    assertEquals(event, service.find(10L));
  }

  @Test
  void findMissingThrowsNotFound() {
    EventRepository repo = mock(EventRepository.class);
    when(repo.findById(99L)).thenReturn(Optional.empty());

    EventService service = new EventService(repo);
    var ex = assertThrows(ResponseStatusException.class, () -> service.find(99L));
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }
}
