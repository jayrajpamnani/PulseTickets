package com.antra.ticket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReserveRequestDTO(
    @NotNull Long eventId,
    @Min(1) int quantity
) {}
