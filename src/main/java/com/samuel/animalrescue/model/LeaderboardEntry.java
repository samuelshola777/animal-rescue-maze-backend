package com.samuel.animalrescue.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LeaderboardEntry(
        UUID gameId,
        String nickname,
        AnimalType animal,
        Difficulty difficulty,
        GameMode mode,
        LocalDate challengeDate,
        int score,
        int rescuedAnimals,
        int collectedStars,
        int completionSeconds,
        Instant achievedAt
) {
}
