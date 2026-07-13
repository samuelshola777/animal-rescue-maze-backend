package com.samuel.animalrescue.exception;

public class GameCapacityException extends RuntimeException {
    public GameCapacityException() {
        super("The game server has reached its temporary session capacity. Please try again later.");
    }
}
