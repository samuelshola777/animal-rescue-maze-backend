package com.samuel.animalrescue.dto;

import com.samuel.animalrescue.model.LeaderboardEntry;

import java.time.Instant;
import java.util.List;

public record LeaderboardResponse(
        List<LeaderboardEntry> entries,
        Instant nextFullResetAt
) {
}
