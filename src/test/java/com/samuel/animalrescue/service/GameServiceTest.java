package com.samuel.animalrescue.service;

import com.samuel.animalrescue.config.GameProperties;
import com.samuel.animalrescue.dto.GameStateResponse;
import com.samuel.animalrescue.dto.MoveRequest;
import com.samuel.animalrescue.dto.StartGameRequest;
import com.samuel.animalrescue.game.MazeGenerator;
import com.samuel.animalrescue.model.AnimalType;
import com.samuel.animalrescue.model.Direction;
import com.samuel.animalrescue.model.Difficulty;
import com.samuel.animalrescue.model.GameMode;
import com.samuel.animalrescue.model.GameStatus;
import com.samuel.animalrescue.model.MoveOutcome;
import com.samuel.animalrescue.model.Position;
import com.samuel.animalrescue.repository.InMemoryGameStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameServiceTest {
    private InMemoryGameStore store;
    private MutableClock clock;
    private GameService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryGameStore();
        clock = new MutableClock(Instant.parse("2026-07-13T10:00:00Z"));
        GameProperties properties = new GameProperties(15, 15, 10, 3, 90, 100, 100, 172_800_000);
        service = new GameService(store, new MazeGenerator(), properties, clock);
    }

    @Test
    void startsAGuestGameAndBlocksMovementThroughWalls() {
        GameStateResponse started = service.start(new StartGameRequest("  Little Hero  ", AnimalType.PANDA, Difficulty.EASY, GameMode.NORMAL));

        GameStateResponse blocked = service.move(started.sessionId(), new MoveRequest(Direction.UP));

        assertThat(started.nickname()).isEqualTo("Little Hero");
        assertThat(started.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(blocked.moveOutcome()).isEqualTo(MoveOutcome.BLOCKED);
        assertThat(blocked.playerPosition()).isEqualTo(started.playerPosition());
        assertThat(blocked.movesMade()).isZero();
    }

    @Test
    void homeRemainsLockedUntilEveryObjectiveIsCompleted() {
        GameStateResponse state = service.start(new StartGameRequest("Explorer", AnimalType.FOX, Difficulty.EASY, GameMode.NORMAL));
        for (Direction direction : pathTo(state, state.homePosition())) {
            state = service.move(state.sessionId(), new MoveRequest(direction));
        }

        assertThat(state.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(state.moveOutcome()).isEqualTo(MoveOutcome.HOME_LOCKED);
        assertThat(service.leaderboard(10).entries()).isEmpty();
    }

    @Test
    void playerWinsAfterCollectingEveryStarAndRescuingEveryAnimal() {
        GameStateResponse state = service.start(new StartGameRequest("Explorer", AnimalType.FOX, Difficulty.EASY, GameMode.NORMAL));
        java.util.List<Position> objectives = new java.util.ArrayList<>(state.remainingStars());
        state.remainingRescueTargets().forEach(target -> objectives.add(target.position()));

        for (Position objective : objectives) {
            for (Direction direction : pathTo(state, objective)) {
                state = service.move(state.sessionId(), new MoveRequest(direction));
            }
        }
        for (Direction direction : pathTo(state, state.homePosition())) {
            state = service.move(state.sessionId(), new MoveRequest(direction));
        }

        assertThat(state.status()).isEqualTo(GameStatus.WON);
        assertThat(state.moveOutcome()).isEqualTo(MoveOutcome.HOME_REACHED);
        assertThat(state.collectedStars()).isEqualTo(state.totalStars());
        assertThat(state.rescuedAnimals()).isEqualTo(state.totalAnimals());
        assertThat(state.score()).isGreaterThanOrEqualTo(5_300);
        assertThat(service.leaderboard(10).entries()).hasSize(1);
    }

    @Test
    void serverClockExpiresTheGame() {
        GameStateResponse started = service.start(new StartGameRequest("Timer", AnimalType.RABBIT, Difficulty.MEDIUM, GameMode.NORMAL));
        clock.advance(Duration.ofSeconds(91));

        GameStateResponse expired = service.get(started.sessionId());

        assertThat(expired.status()).isEqualTo(GameStatus.EXPIRED);
        assertThat(expired.secondsRemaining()).isZero();
        assertThatThrownBy(() -> service.move(started.sessionId(), new MoveRequest(Direction.RIGHT)))
                .isInstanceOf(com.samuel.animalrescue.exception.GameNotActiveException.class);
    }

    @Test
    void fullResetDeletesSessionsAndScores() {
        GameStateResponse started = service.start(new StartGameRequest("Reset", AnimalType.PUPPY, Difficulty.EASY, GameMode.NORMAL));

        service.clearAllTemporaryData();

        assertThat(store.sessionCount()).isZero();
        assertThat(service.leaderboard(10).entries()).isEmpty();
        assertThatThrownBy(() -> service.get(started.sessionId()))
                .isInstanceOf(com.samuel.animalrescue.exception.GameNotFoundException.class);
    }

    @Test
    void dailyChallengeUsesTheSameMazeAndSeparateMode() {
        GameStateResponse first = service.start(new StartGameRequest("One", AnimalType.FOX, Difficulty.MEDIUM, GameMode.DAILY));
        GameStateResponse second = service.start(new StartGameRequest("Two", AnimalType.PANDA, Difficulty.MEDIUM, GameMode.DAILY));

        assertThat(first.mode()).isEqualTo(GameMode.DAILY);
        assertThat(first.challengeDate()).isEqualTo(second.challengeDate());
        assertThat(first.maze()).isEqualTo(second.maze());
        assertThat(first.remainingStars()).isEqualTo(second.remainingStars());
        assertThat(first.enemyPositions()).isEqualTo(second.enemyPositions());
    }

    @Test
    void mediumGameIncludesEnemyAndPowerUps() {
        GameStateResponse state = service.start(new StartGameRequest("Features", AnimalType.LION_CUB, Difficulty.MEDIUM, GameMode.NORMAL));

        assertThat(state.enemyPositions()).hasSize(1);
        assertThat(state.remainingPowerUps()).hasSize(4);
        assertThat(state.difficulty()).isEqualTo(Difficulty.MEDIUM);
    }

    private java.util.List<Direction> pathTo(GameStateResponse state, Position destination) {
        Position start = state.playerPosition();
        Queue<Position> queue = new ArrayDeque<>();
        Map<Position, Position> previous = new HashMap<>();
        Map<Position, Direction> directionUsed = new HashMap<>();
        queue.add(start);
        previous.put(start, null);

        while (!queue.isEmpty()) {
            Position current = queue.remove();
            if (current.equals(destination)) break;
            for (Direction direction : Direction.values()) {
                Position next = current.move(direction);
                if (!isWall(state.maze(), next) && !previous.containsKey(next)) {
                    previous.put(next, current);
                    directionUsed.put(next, direction);
                    queue.add(next);
                }
            }
        }

        java.util.LinkedList<Direction> path = new java.util.LinkedList<>();
        for (Position current = destination; !current.equals(start); current = previous.get(current)) {
            path.addFirst(directionUsed.get(current));
        }
        return path;
    }

    private boolean isWall(java.util.List<String> maze, Position position) {
        return position.row() < 0 || position.row() >= maze.size()
                || position.column() < 0 || position.column() >= maze.get(0).length()
                || maze.get(position.row()).charAt(position.column()) == '#';
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
