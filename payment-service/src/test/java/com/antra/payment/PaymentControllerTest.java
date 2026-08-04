package com.antra.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

class PaymentControllerTest {
  @Test void duplicatePaymentIsRejected() {
    PaymentRepository repository = mock(PaymentRepository.class);
    when(repository.findByReservationId(7L)).thenReturn(Optional.of(mock(Payment.class)));
    PaymentController controller = new PaymentController(repository, mock(org.springframework.kafka.core.KafkaTemplate.class));
    var response = controller.pay(mock(Authentication.class), 7L, BigDecimal.TEN);
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
  }
}
