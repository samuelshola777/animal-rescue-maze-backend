package com.samuel.animalrescue.model;

import com.samuel.animalrescue.game.Maze;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GameSession {
    private final UUID id;
    private final String nickname;
    private final AnimalType selectedAnimal;
    private final Difficulty difficulty;
    private final GameMode mode;
    private final LocalDate challengeDate;
    private final Maze maze;
    private final Instant startedAt;
    private final int totalStars;
    private final int totalAnimals;
    private final Set<Position> remainingStars;
    private final Map<Position, AnimalType> remainingRescueTargets;
    private final Map<Position, PowerUpType> remainingPowerUps;
    private final List<EnemyState> enemies;
    private final Set<Position> bushPositions;
    private final Position keyPosition;
    private final Position gatePosition;
    private final List<AnimalType> rescuedFollowers = new ArrayList<>();

    private Instant endsAt;
    private Position playerPosition;
    private GameStatus status;
    private Instant lastActivityAt;
    private Instant finishedAt;
    private Instant protectedUntil;
    private Position bananaDecoyPosition;
    private int bananaDecoyTurnsRemaining;
    private int bananaCount;
    private int collectedStars;
    private int rescuedAnimals;
    private int score;
    private int movesMade;
    private int comboMultiplier = 1;
    private int lastCollectMove = -100;
    private int wallVisionMovesRemaining;
    private int speedBoostMovesRemaining;
    private int enemyHits;
    private int lastRabbitJumpMove = -100;
    private int lionShieldCharges;
    private int shieldCharges;
    private int abilityCooldownMovesRemaining;
    private boolean rabbitJumpReady;
    private boolean keyCollected;

    public GameSession(UUID id,
                       String nickname,
                       AnimalType selectedAnimal,
                       Difficulty difficulty,
                       GameMode mode,
                       LocalDate challengeDate,
                       Maze maze,
                       Map<Position, PowerUpType> powerUps,
                       Set<Position> enemySpawns,
                       Set<Position> bushPositions,
                       Position keyPosition,
                       Position gatePosition,
                       Instant startedAt,
                       Instant endsAt) {
        this.id = id;
        this.nickname = nickname;
        this.selectedAnimal = selectedAnimal;
        this.difficulty = difficulty;
        this.mode = mode;
        this.challengeDate = challengeDate;
        this.maze = maze;
        this.startedAt = startedAt;
        this.endsAt = selectedAnimal == AnimalType.PANDA ? endsAt.plusSeconds(15) : endsAt;
        this.totalStars = maze.stars().size();
        this.totalAnimals = maze.rescueTargets().size();
        this.remainingStars = new LinkedHashSet<>(maze.stars());
        this.remainingRescueTargets = new LinkedHashMap<>(maze.rescueTargets());
        this.remainingPowerUps = new LinkedHashMap<>(powerUps);
        this.enemies = enemySpawns.stream().map(EnemyState::new).toList();
        this.bushPositions = new LinkedHashSet<>(bushPositions);
        this.keyPosition = keyPosition;
        this.gatePosition = gatePosition;
        this.playerPosition = maze.start();
        this.status = GameStatus.ACTIVE;
        this.lastActivityAt = startedAt;
        this.lionShieldCharges = selectedAnimal == AnimalType.LION_CUB ? 2 : 0;
        this.bananaCount = difficulty == Difficulty.EASY ? 0 : 2;
    }

    public UUID id() { return id; }
    public String nickname() { return nickname; }
    public AnimalType selectedAnimal() { return selectedAnimal; }
    public Difficulty difficulty() { return difficulty; }
    public GameMode mode() { return mode; }
    public LocalDate challengeDate() { return challengeDate; }
    public Maze maze() { return maze; }
    public Instant startedAt() { return startedAt; }
    public Instant endsAt() { return endsAt; }
    public int totalStars() { return totalStars; }
    public int totalAnimals() { return totalAnimals; }
    public Position playerPosition() { return playerPosition; }
    public GameStatus status() { return status; }
    public Instant lastActivityAt() { return lastActivityAt; }
    public Instant finishedAt() { return finishedAt; }
    public int collectedStars() { return collectedStars; }
    public int rescuedAnimals() { return rescuedAnimals; }
    public int score() { return score; }
    public int movesMade() { return movesMade; }
    public int comboMultiplier() { return comboMultiplier; }
    public int wallVisionMovesRemaining() { return wallVisionMovesRemaining; }
    public int speedBoostMovesRemaining() { return speedBoostMovesRemaining; }
    public int enemyHits() { return enemyHits; }
    public int shieldCharges() { return shieldCharges; }
    public int abilityCooldownMovesRemaining() { return abilityCooldownMovesRemaining; }
    public int bananaCount() { return bananaCount; }
    public Position bananaDecoyPosition() { return bananaDecoyPosition; }
    public int bananaDecoyTurnsRemaining() { return bananaDecoyTurnsRemaining; }
    public Position keyPosition() { return keyPosition; }
    public Position gatePosition() { return gatePosition; }
    public boolean keyCollected() { return keyCollected; }
    public boolean abilityReady() { return abilityCooldownMovesRemaining == 0; }
    public boolean hiddenInBush() { return bushPositions.contains(playerPosition); }
    public boolean allObjectivesCompleted() { return collectedStars == totalStars && rescuedAnimals == totalAnimals; }

    public Set<Position> remainingStars() { return Set.copyOf(remainingStars); }
    public Map<Position, AnimalType> remainingRescueTargets() { return Map.copyOf(remainingRescueTargets); }
    public Map<Position, PowerUpType> remainingPowerUps() { return Map.copyOf(remainingPowerUps); }
    public List<EnemyState> enemies() { return List.copyOf(enemies); }
    public Set<Position> enemyPositions() { return enemies.stream().map(EnemyState::position).collect(java.util.stream.Collectors.toUnmodifiableSet()); }
    public Set<Position> bushPositions() { return Set.copyOf(bushPositions); }
    public List<AnimalType> rescuedFollowers() { return List.copyOf(rescuedFollowers); }

    public void moveTo(Position position, Instant now) {
        playerPosition = position;
        lastActivityAt = now;
        movesMade++;
        if (wallVisionMovesRemaining > 0) wallVisionMovesRemaining--;
        if (speedBoostMovesRemaining > 0) speedBoostMovesRemaining--;
        if (abilityCooldownMovesRemaining > 0) abilityCooldownMovesRemaining--;
        if (movesMade - lastCollectMove > 4) comboMultiplier = 1;
    }

    public boolean canRabbitJump() {
        return selectedAnimal == AnimalType.RABBIT
                && (rabbitJumpReady || movesMade - lastRabbitJumpMove >= 8);
    }

    public void useRabbitJump() {
        rabbitJumpReady = false;
        lastRabbitJumpMove = movesMade;
    }

    public boolean collectStarAt(Position position) {
        if (!remainingStars.remove(position)) return false;
        collectedStars++;
        awardComboPoints(100);
        return true;
    }

    public AnimalType rescueAt(Position position) {
        AnimalType rescued = remainingRescueTargets.remove(position);
        if (rescued == null) return null;
        rescuedAnimals++;
        rescuedFollowers.add(rescued);
        awardComboPoints(500);
        return rescued;
    }

    public boolean collectKeyAt(Position position) {
        if (keyCollected || keyPosition == null || !keyPosition.equals(position)) return false;
        keyCollected = true;
        score += 250;
        return true;
    }

    public boolean canPassGate(Position position) {
        return keyCollected && gatePosition != null && gatePosition.equals(position);
    }

    public PowerUpType collectPowerUpAt(Position position) {
        PowerUpType powerUp = remainingPowerUps.remove(position);
        if (powerUp == null) return null;
        score += 150;
        switch (powerUp) {
            case EXTRA_TIME -> endsAt = endsAt.plusSeconds(10);
            case WALL_VISION -> wallVisionMovesRemaining = Math.max(wallVisionMovesRemaining, 10);
            case SPEED_BOOST -> speedBoostMovesRemaining = Math.max(speedBoostMovesRemaining, 10);
            case SHIELD -> shieldCharges++;
            case STAR_MAGNET -> { }
        }
        return powerUp;
    }

    public boolean enemiesShouldMove() {
        if (movesMade % 2 != 0) return false;
        if (speedBoostMovesRemaining > 0 || selectedAnimal == AnimalType.FOX) return movesMade % 4 == 0;
        return true;
    }

    public boolean dropBanana() {
        if (bananaCount <= 0) return false;
        bananaCount--;
        bananaDecoyPosition = playerPosition;
        bananaDecoyTurnsRemaining = 4;
        return true;
    }

    public void advanceBananaDecoy() {
        if (bananaDecoyTurnsRemaining > 0) bananaDecoyTurnsRemaining--;
        if (bananaDecoyTurnsRemaining == 0) bananaDecoyPosition = null;
    }

    public boolean useAbility(Instant now) {
        if (!abilityReady()) return false;
        switch (selectedAnimal) {
            case FOX -> speedBoostMovesRemaining = Math.max(speedBoostMovesRemaining, 8);
            case PANDA -> endsAt = endsAt.plusSeconds(10);
            case RABBIT -> rabbitJumpReady = true;
            case LION_CUB -> enemies.forEach(EnemyState::resetToSpawn);
            case PUPPY -> wallVisionMovesRemaining = Math.max(wallVisionMovesRemaining, 10);
        }
        abilityCooldownMovesRemaining = 12;
        lastActivityAt = now;
        return true;
    }

    public boolean isProtected(Instant now) {
        return protectedUntil != null && now.isBefore(protectedUntil);
    }

    public long protectionSecondsRemaining(Instant now) {
        if (!isProtected(now)) return 0;
        long milliseconds = java.time.Duration.between(now, protectedUntil).toMillis();
        return (milliseconds + 999) / 1_000;
    }

    public boolean consumeShield() {
        if (shieldCharges <= 0) return false;
        shieldCharges--;
        return true;
    }

    public boolean consumeLionShield() {
        if (lionShieldCharges <= 0) return false;
        lionShieldCharges--;
        return true;
    }

    public void applyEnemyHit(Instant now) {
        enemyHits++;
        comboMultiplier = 1;
        endsAt = endsAt.minusSeconds(5);
        if (endsAt.isBefore(now)) endsAt = now;
        protectedUntil = now.plusSeconds(3);
        lastActivityAt = now;
    }

    public void win(Instant now, int remainingSeconds) {
        status = GameStatus.WON;
        finishedAt = now;
        lastActivityAt = now;
        score += 1_000 + Math.max(0, remainingSeconds) * 10 + 1_000 + 500;
        if (enemyHits == 0) score += 1_000;
    }

    public void expire(Instant now) {
        if (status == GameStatus.ACTIVE) {
            status = GameStatus.EXPIRED;
            finishedAt = now;
            lastActivityAt = now;
        }
    }

    public void abandon(Instant now) {
        if (status == GameStatus.ACTIVE) {
            status = GameStatus.ABANDONED;
            finishedAt = now;
            lastActivityAt = now;
        }
    }

    private void awardComboPoints(int basePoints) {
        comboMultiplier = movesMade - lastCollectMove <= 4 ? Math.min(5, comboMultiplier + 1) : 1;
        lastCollectMove = movesMade;
        score += basePoints * comboMultiplier;
    }
}
