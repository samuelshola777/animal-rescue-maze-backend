package com.samuel.animalrescue.model;

public enum Difficulty {
    EASY(11, 11, 6, 2, 120, 0, 2),
    MEDIUM(15, 15, 10, 3, 90, 1, 4),
    HARD(19, 19, 14, 4, 75, 2, 5);

    private final int rows;
    private final int columns;
    private final int stars;
    private final int rescues;
    private final int durationSeconds;
    private final int enemies;
    private final int powerUps;

    Difficulty(int rows, int columns, int stars, int rescues, int durationSeconds, int enemies, int powerUps) {
        this.rows = rows;
        this.columns = columns;
        this.stars = stars;
        this.rescues = rescues;
        this.durationSeconds = durationSeconds;
        this.enemies = enemies;
        this.powerUps = powerUps;
    }

    public int rows() { return rows; }
    public int columns() { return columns; }
    public int stars() { return stars; }
    public int rescues() { return rescues; }
    public int durationSeconds() { return durationSeconds; }
    public int enemies() { return enemies; }
    public int powerUps() { return powerUps; }
}
