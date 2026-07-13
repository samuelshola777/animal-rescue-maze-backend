package com.samuel.animalrescue.dto;

import com.samuel.animalrescue.model.Difficulty;

public record DifficultyOption(
        Difficulty difficulty,
        int rows,
        int columns,
        int stars,
        int animalsToRescue,
        int durationSeconds,
        int enemies,
        int powerUps
) {
}
