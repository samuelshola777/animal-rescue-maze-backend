package com.samuel.animalrescue.dto;

import com.samuel.animalrescue.model.AnimalType;
import com.samuel.animalrescue.model.GameStatus;
import com.samuel.animalrescue.model.Difficulty;
import com.samuel.animalrescue.model.GameMode;
import com.samuel.animalrescue.model.MoveOutcome;
import com.samuel.animalrescue.model.Position;
import com.samuel.animalrescue.model.RescueTarget;
import com.samuel.animalrescue.model.PowerUp;

import java.time.LocalDate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GameStateResponse(
        UUID sessionId,
        String nickname,
        AnimalType selectedAnimal,
        Difficulty difficulty,
        GameMode mode,
        LocalDate challengeDate,
        GameStatus status,
        MoveOutcome moveOutcome,
        List<String> maze,
        Position playerPosition,
        Position homePosition,
        List<Position> remainingStars,
        List<RescueTarget> remainingRescueTargets,
        List<PowerUp> remainingPowerUps,
        List<Position> enemyPositions,
        List<Position> bushPositions,
        Position keyPosition,
        Position gatePosition,
        boolean keyCollected,
        Position bananaDecoyPosition,
        int bananaCount,
        int shieldCharges,
        long protectionSecondsRemaining,
        boolean abilityReady,
        int abilityCooldownMovesRemaining,
        List<AnimalType> rescuedFollowers,
        boolean enemyWarning,
        Integer nearestEnemyDistance,
        int collectedStars,
        int totalStars,
        int rescuedAnimals,
        int totalAnimals,
        int score,
        int comboMultiplier,
        int wallVisionMovesRemaining,
        int speedBoostMovesRemaining,
        int enemyHits,
        Position nearestObjective,
        List<String> events,
        long secondsRemaining,
        int movesMade,
        Instant startedAt,
        Instant endsAt
) {
}
