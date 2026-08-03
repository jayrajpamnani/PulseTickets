package com.antra.payment;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentRepository extends JpaRepository<Payment, Long> {
  Optional<Payment> findByReservationId(Long id);
}
