package com.samuel.animalrescue.dto;

import com.samuel.animalrescue.model.GameActionType;
import jakarta.validation.constraints.NotNull;

public record GameActionRequest(@NotNull GameActionType action) {
}
