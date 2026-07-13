package com.samuel.animalrescue.exception;

import com.samuel.animalrescue.model.GameStatus;

public class GameNotActiveException extends RuntimeException {
    public GameNotActiveException(GameStatus status) {
        super("This game is no longer active. Current status: " + status);
    }
}
