package com.antra.payment;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/payments")
class PaymentController {
  private final PaymentRepository repo;
  private final KafkaTemplate<String, Object> kafka;
  PaymentController(PaymentRepository repo, KafkaTemplate<String, Object> kafka) { this.repo = repo; this.kafka = kafka; }

  @PostMapping
  ResponseEntity<Payment> pay(Authentication auth, @RequestParam Long reservationId,
      @RequestParam @DecimalMin("0.01") BigDecimal amount) {
    if (repo.findByReservationId(reservationId).isPresent()) return ResponseEntity.status(409).build();
    Payment payment = repo.save(new Payment(reservationId, auth.getName(), amount));
    kafka.send("payment-completed", reservationId.toString(), Map.of("reservationId", reservationId,
        "username", auth.getName(), "amount", amount));
    return ResponseEntity.status(201).body(payment);
  }

  @GetMapping("/{reservationId}") Payment status(@PathVariable Long reservationId, Authentication auth) {
    Payment payment = repo.findByReservationId(reservationId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!payment.username.equals(auth.getName())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return payment;
  }
}
