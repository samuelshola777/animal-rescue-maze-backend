package com.samuel.animalrescue.model;

public record Position(int row, int column) {

    public Position move(Direction direction) {
        return new Position(row + direction.rowDelta(), column + direction.columnDelta());
    }
}
