package com.antra.event.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateEventDTO(
    @NotBlank String title,
    @NotBlank String venue,
    @Future Instant startsAt,
    @NotNull @DecimalMin("0.01") BigDecimal price,
    @Min(1) int capacity,
    String description,
    String bannerUrl
) {}
