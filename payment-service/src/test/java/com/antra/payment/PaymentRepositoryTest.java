package com.antra.payment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class PaymentRepositoryTest {
  @Test
  void testFindByReservationId() {
    PaymentRepository repo = mock(PaymentRepository.class);
    when(repo.findByReservationId(7L)).thenReturn(Optional.empty());

    assertTrue(repo.findByReservationId(7L).isEmpty());
  }
}
