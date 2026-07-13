package com.samuel.animalrescue.dto;

import java.time.Instant;

public record KeepAliveResponse(
        String status,
        String message,
        Instant timestamp
) {
}