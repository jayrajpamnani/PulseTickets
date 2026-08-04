package com.antra.event.dto;

import java.math.BigDecimal;

public record ReservationResponseDTO(
    Long eventId,
    String title,
    BigDecimal unitPrice,
    int quantity
) {}
