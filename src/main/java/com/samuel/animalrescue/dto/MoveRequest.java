package com.samuel.animalrescue.dto;

import com.samuel.animalrescue.model.Direction;
import jakarta.validation.constraints.NotNull;

public record MoveRequest(@NotNull Direction direction) {
}
