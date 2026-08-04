package com.antra.payment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

class PaymentControllerTest {
  @Test
  void duplicatePaymentIsRejected() {
    PaymentService service = mock(PaymentService.class);
    when(service.pay("alice", 7L, BigDecimal.TEN)).thenReturn(Optional.empty());

    PaymentController controller = new PaymentController(service);
    var auth = new UsernamePasswordAuthenticationToken("alice", null);
    var response = controller.pay(auth, 7L, BigDecimal.TEN);
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
  }

  @Test
  void payWithoutAuthThrowsUnauthorized() {
    PaymentService service = mock(PaymentService.class);
    PaymentController controller = new PaymentController(service);

    assertThrows(ResponseStatusException.class, () -> controller.pay(null, 7L, BigDecimal.TEN));
  }
}
