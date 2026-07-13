package com.samuel.animalrescue.dto;

import com.samuel.animalrescue.model.AnimalType;
import com.samuel.animalrescue.model.GameMode;
import com.samuel.animalrescue.model.PowerUpType;

import java.util.List;
import java.util.Map;

public record GameOptionsResponse(
        List<AnimalType> animals,
        List<DifficultyOption> difficulties,
        List<GameMode> modes,
        List<PowerUpType> powerUps,
        Map<AnimalType, String> animalAbilities,
        long dataResetHours,
        String wallCharacter,
        String pathCharacter
) {
}
