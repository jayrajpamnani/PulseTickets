package com.antra.ticket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReservationRepositoryTest {
  @Test
  void testFindByUsername() {
    ReservationRepository repo = mock(ReservationRepository.class);
    when(repo.findByUsernameOrderByCreatedAtDesc("alice")).thenReturn(List.of());

    var results = repo.findByUsernameOrderByCreatedAtDesc("alice");
    assertNotNull(results);
    assertTrue(results.isEmpty());
  }
}
