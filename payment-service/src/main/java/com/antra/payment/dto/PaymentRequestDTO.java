package com.antra.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PaymentRequestDTO(
    @NotNull Long reservationId,
    @NotNull @DecimalMin("0.01") BigDecimal amount
) {}
