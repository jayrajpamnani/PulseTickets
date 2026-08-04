package com.antra.payment;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {
  private final PaymentRepository repo;
  private final KafkaTemplate<String, Object> kafka;

  public PaymentService(PaymentRepository repo, KafkaTemplate<String, Object> kafka) {
    this.repo = repo;
    this.kafka = kafka;
  }

  @Transactional
  public Optional<Payment> pay(String username, Long reservationId, BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be positive");
    }
    if (repo.findByReservationId(reservationId).isPresent()) {
      return Optional.empty();
    }
    Payment payment = repo.save(new Payment(reservationId, username, amount));
    kafka.send("payment-completed", reservationId.toString(), Map.of(
        "reservationId", reservationId,
        "username", username,
        "amount", amount
    ));
    return Optional.of(payment);
  }

  public Payment getStatus(Long reservationId, String username) {
    Payment payment = repo.findByReservationId(reservationId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    if (!payment.username.equals(username)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
    return payment;
  }
}
