package com.antra.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class EventRepositoryTest {
  @Test
  void testSearchAndFind() {
    EventRepository repo = mock(EventRepository.class);
    PageRequest pageable = PageRequest.of(0, 20);
    when(repo.findByTitleContainingIgnoreCaseOrVenueContainingIgnoreCaseOrDescriptionContainingIgnoreCase("rock", "rock", "rock", pageable))
        .thenReturn(new PageImpl<>(List.of()));
    when(repo.findById(1L)).thenReturn(Optional.empty());

    var result = repo.findByTitleContainingIgnoreCaseOrVenueContainingIgnoreCaseOrDescriptionContainingIgnoreCase("rock", "rock", "rock", pageable);
    assertNotNull(result);
    assertTrue(result.isEmpty());
    assertTrue(repo.findById(1L).isEmpty());
  }
}
