package com.samuel.animalrescue.dto;

import com.samuel.animalrescue.model.AnimalType;
import com.samuel.animalrescue.model.Difficulty;
import com.samuel.animalrescue.model.GameMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StartGameRequest(
        @NotBlank
        @Size(min = 2, max = 20)
        @Pattern(regexp = "^[\\p{L}\\p{N} _-]+$", message = "nickname contains unsupported characters")
        String nickname,

        @NotNull AnimalType animal,
        Difficulty difficulty,
        GameMode mode
) {
    public StartGameRequest(String nickname, AnimalType animal) {
        this(nickname, animal, Difficulty.MEDIUM, GameMode.NORMAL);
    }
}
