package com.antra.payment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;

class PaymentServiceTest {
  private PaymentRepository repo;
  private KafkaTemplate<String, Object> kafka;
  private PaymentService service;

  @BeforeEach
  void setUp() {
    repo = mock(PaymentRepository.class);
    kafka = mock(KafkaTemplate.class);
    service = new PaymentService(repo, kafka);
  }

  @Test
  void paySuccess() {
    when(repo.findByReservationId(7L)).thenReturn(Optional.empty());
    when(repo.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

    var result = service.pay("alice", 7L, BigDecimal.TEN);
    assertTrue(result.isPresent());
    assertEquals("SUCCESS", result.get().status);
    verify(kafka).send(eq("payment-completed"), eq("7"), any());
  }

  @Test
  void payDuplicateReturnsEmpty() {
    Payment existing = mock(Payment.class);
    when(repo.findByReservationId(7L)).thenReturn(Optional.of(existing));

    var result = service.pay("alice", 7L, BigDecimal.TEN);
    assertTrue(result.isEmpty());
    verify(kafka, never()).send(anyString(), anyString(), any());
  }

  @Test
  void getStatusForbiddenForOtherUser() {
    Payment payment = new Payment(7L, "bob", BigDecimal.TEN);
    when(repo.findByReservationId(7L)).thenReturn(Optional.of(payment));

    var ex = assertThrows(ResponseStatusException.class, () -> service.getStatus(7L, "alice"));
    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
  }
}
