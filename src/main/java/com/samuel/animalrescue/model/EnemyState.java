package com.samuel.animalrescue.model;

import java.util.UUID;

public final class EnemyState {
    private final UUID id;
    private final Position spawnPosition;
    private Position position;
    private int chaseMovesRemaining;

    public EnemyState(Position spawnPosition) {
        this.id = UUID.nameUUIDFromBytes((spawnPosition.row() + ":" + spawnPosition.column()).getBytes());
        this.spawnPosition = spawnPosition;
        this.position = spawnPosition;
    }

    public UUID id() { return id; }
    public Position spawnPosition() { return spawnPosition; }
    public Position position() { return position; }
    public int chaseMovesRemaining() { return chaseMovesRemaining; }
    public boolean chasing() { return chaseMovesRemaining > 0; }

    public void moveTo(Position destination) { this.position = destination; }
    public void beginChase() { this.chaseMovesRemaining = 3; }
    public void stopChase() { this.chaseMovesRemaining = 0; }
    public void advanceChase() { if (chaseMovesRemaining > 0) chaseMovesRemaining--; }

    public void resetToSpawn() {
        this.position = spawnPosition;
        this.chaseMovesRemaining = 0;
    }
}
