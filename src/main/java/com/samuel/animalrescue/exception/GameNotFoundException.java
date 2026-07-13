package com.samuel.animalrescue.exception;

import java.util.UUID;

public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(UUID id) {
        super("Game session " + id + " was not found");
    }
}
