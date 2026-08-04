package com.antra.payment;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping
  public ResponseEntity<Payment> pay(
      Authentication auth,
      @RequestParam Long reservationId,
      @RequestParam @DecimalMin("0.01") BigDecimal amount) {
    if (auth == null || auth.getName() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
    }
    return paymentService.pay(auth.getName(), reservationId, amount)
        .map(p -> ResponseEntity.status(HttpStatus.CREATED).body(p))
        .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT).build());
  }

  @GetMapping("/{reservationId}")
  public Payment status(@PathVariable Long reservationId, Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
    }
    return paymentService.getStatus(reservationId, auth.getName());
  }
}
