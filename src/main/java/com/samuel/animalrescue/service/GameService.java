package com.samuel.animalrescue.service;

import com.samuel.animalrescue.config.GameProperties;
import com.samuel.animalrescue.dto.DifficultyOption;
import com.samuel.animalrescue.dto.GameActionRequest;
import com.samuel.animalrescue.dto.GameOptionsResponse;
import com.samuel.animalrescue.dto.GameStateResponse;
import com.samuel.animalrescue.dto.LeaderboardResponse;
import com.samuel.animalrescue.dto.MoveRequest;
import com.samuel.animalrescue.dto.StartGameRequest;
import com.samuel.animalrescue.exception.GameCapacityException;
import com.samuel.animalrescue.exception.GameNotActiveException;
import com.samuel.animalrescue.exception.GameNotFoundException;
import com.samuel.animalrescue.game.Maze;
import com.samuel.animalrescue.game.MazeGenerator;
import com.samuel.animalrescue.model.AnimalType;
import com.samuel.animalrescue.model.Difficulty;
import com.samuel.animalrescue.model.Direction;
import com.samuel.animalrescue.model.EnemyState;
import com.samuel.animalrescue.model.GameActionType;
import com.samuel.animalrescue.model.GameMode;
import com.samuel.animalrescue.model.GameSession;
import com.samuel.animalrescue.model.GameStatus;
import com.samuel.animalrescue.model.LeaderboardEntry;
import com.samuel.animalrescue.model.MoveOutcome;
import com.samuel.animalrescue.model.Position;
import com.samuel.animalrescue.model.PowerUp;
import com.samuel.animalrescue.model.PowerUpType;
import com.samuel.animalrescue.model.RescueTarget;
import com.samuel.animalrescue.repository.InMemoryGameStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class GameService {
    private static final int CHASE_DISTANCE = 3;
    private static final int WARNING_DISTANCE = 4;
    private static final int SAFE_ZONE_DISTANCE = 2;

    private final InMemoryGameStore store;
    private final MazeGenerator mazeGenerator;
    private final GameProperties properties;
    private final Clock clock;
    private volatile Instant nextFullResetAt;

    public GameService(InMemoryGameStore store, MazeGenerator mazeGenerator,
                       GameProperties properties, Clock clock) {
        this.store = store;
        this.mazeGenerator = mazeGenerator;
        this.properties = properties;
        this.clock = clock;
        this.nextFullResetAt = clock.instant().plusMillis(properties.fullResetMilliseconds());
    }

    public GameStateResponse start(StartGameRequest request) {
        if (store.sessionCount() >= properties.maximumSessions()) throw new GameCapacityException();
        Instant now = clock.instant();
        Difficulty difficulty = request.difficulty() == null ? Difficulty.MEDIUM : request.difficulty();
        GameMode mode = request.mode() == null ? GameMode.NORMAL : request.mode();
        LocalDate challengeDate = mode == GameMode.DAILY ? LocalDate.now(clock.withZone(ZoneOffset.UTC)) : null;
        long seed = mode == GameMode.DAILY ? dailySeed(challengeDate, difficulty) : ThreadLocalRandom.current().nextLong();
        Maze maze = mazeGenerator.generate(difficulty.rows(), difficulty.columns(), difficulty.stars(), difficulty.rescues(), seed);
        GameExtras extras = createExtras(maze, difficulty, seed);
        GameSession session = new GameSession(UUID.randomUUID(), normalizeNickname(request.nickname()),
                request.animal(), difficulty, mode, challengeDate, maze, extras.powerUps(), extras.enemies(),
                extras.bushes(), extras.keyPosition(), extras.gatePosition(), now,
                now.plusSeconds(difficulty.durationSeconds()));
        store.save(session);
        List<String> events = request.animal() == AnimalType.PANDA ? List.of("PANDA_TIME_BONUS") : List.of();
        return toResponse(session, null, events, now);
    }

    public GameStateResponse get(UUID sessionId) {
        GameSession session = requireSession(sessionId);
        synchronized (session) {
            Instant now = clock.instant();
            expireIfNecessary(session, now);
            return toResponse(session, null, List.of(), now);
        }
    }

    public GameStateResponse move(UUID sessionId, MoveRequest request) {
        GameSession session = requireSession(sessionId);
        synchronized (session) {
            Instant now = clock.instant();
            List<String> events = new ArrayList<>();
            if (expireIfNecessary(session, now)) return toResponse(session, MoveOutcome.TIME_EXPIRED, List.of("TIME_EXPIRED"), now);
            requireActive(session);

            Position destination = session.playerPosition().move(request.direction());
            boolean blocked = session.maze().isWall(destination) && !session.canPassGate(destination);
            if (blocked) {
                Position jumpDestination = destination.move(request.direction());
                if (session.canRabbitJump() && !session.maze().isWall(jumpDestination)) {
                    destination = jumpDestination;
                    session.useRabbitJump();
                    events.add("RABBIT_WALL_JUMP");
                } else {
                    return toResponse(session, MoveOutcome.BLOCKED, List.of("WALL_BLOCKED"), now);
                }
            }

            session.moveTo(destination, now);
            MoveOutcome outcome = MoveOutcome.MOVED;
            if (session.collectStarAt(destination)) {
                outcome = MoveOutcome.STAR_COLLECTED;
                events.add("STAR_COLLECTED");
            }
            AnimalType rescued = session.rescueAt(destination);
            if (rescued != null) {
                outcome = MoveOutcome.ANIMAL_RESCUED;
                events.add("ANIMAL_RESCUED_" + rescued.name());
            }
            if (session.collectKeyAt(destination)) events.add("KEY_COLLECTED");

            PowerUpType powerUp = session.collectPowerUpAt(destination);
            if (powerUp != null) {
                outcome = MoveOutcome.POWER_UP_COLLECTED;
                events.add("POWER_UP_" + powerUp.name());
                if (powerUp == PowerUpType.STAR_MAGNET) events.add("MAGNET_STARS_" + collectNearbyStars(session, destination));
            }

            if (destination.equals(session.maze().home())) {
                if (session.allObjectivesCompleted()) {
                    session.win(now, Math.toIntExact(secondsRemaining(session, now)));
                    recordWin(session);
                    events.add("HOME_REACHED");
                    return toResponse(session, MoveOutcome.HOME_REACHED, events, now);
                }
                outcome = MoveOutcome.HOME_LOCKED;
                events.add("HOME_LOCKED");
            }

            if (handleCollisions(session, now, events)) outcome = MoveOutcome.ENEMY_HIT;
            if (session.enemiesShouldMove()) {
                moveEnemies(session);
                if (handleCollisions(session, now, events)) outcome = MoveOutcome.ENEMY_HIT;
            }
            if (expireIfNecessary(session, now)) outcome = MoveOutcome.TIME_EXPIRED;
            return toResponse(session, outcome, events, now);
        }
    }

    public GameStateResponse action(UUID sessionId, GameActionRequest request) {
        GameSession session = requireSession(sessionId);
        synchronized (session) {
            Instant now = clock.instant();
            if (expireIfNecessary(session, now)) return toResponse(session, MoveOutcome.TIME_EXPIRED, List.of("TIME_EXPIRED"), now);
            requireActive(session);
            boolean used;
            String event;
            if (request.action() == GameActionType.DROP_BANANA) {
                used = session.dropBanana();
                event = "BANANA_DROPPED";
            } else {
                used = session.useAbility(now);
                event = "ABILITY_" + session.selectedAnimal().name();
            }
            return toResponse(session, used ? MoveOutcome.ACTION_USED : MoveOutcome.ACTION_UNAVAILABLE,
                    List.of(used ? event : "ACTION_UNAVAILABLE"), now);
        }
    }

    public void abandon(UUID sessionId) {
        GameSession session = requireSession(sessionId);
        synchronized (session) {
            Instant now = clock.instant();
            if (!expireIfNecessary(session, now)) session.abandon(now);
        }
    }

    public LeaderboardResponse leaderboard(int requestedLimit) { return leaderboard(requestedLimit, null, null); }

    public LeaderboardResponse leaderboard(int requestedLimit, GameMode mode, Difficulty difficulty) {
        int limit = Math.max(1, Math.min(requestedLimit, 100));
        LocalDate date = mode == GameMode.DAILY ? LocalDate.now(clock.withZone(ZoneOffset.UTC)) : null;
        return new LeaderboardResponse(store.leaderboard(limit, mode, difficulty, date), nextFullResetAt);
    }

    public GameOptionsResponse options() {
        List<DifficultyOption> difficultyOptions = Arrays.stream(Difficulty.values())
                .map(value -> new DifficultyOption(value, value.rows(), value.columns(), value.stars(),
                        value.rescues(), value.durationSeconds(), value.enemies(), value.powerUps())).toList();
        Map<AnimalType, String> abilities = new EnumMap<>(AnimalType.class);
        abilities.put(AnimalType.FOX, "Dash: enemies move less often for eight moves.");
        abilities.put(AnimalType.PANDA, "Snack break: add ten seconds to the clock.");
        abilities.put(AnimalType.RABBIT, "Super jump: prepare an instant wall jump.");
        abilities.put(AnimalType.LION_CUB, "Big roar: send every monkey back to its patrol start.");
        abilities.put(AnimalType.PUPPY, "Super sniff: reveal the nearest remaining objective.");
        return new GameOptionsResponse(Arrays.asList(AnimalType.values()), difficultyOptions,
                Arrays.asList(GameMode.values()), Arrays.asList(PowerUpType.values()), abilities,
                Duration.ofMillis(properties.fullResetMilliseconds()).toHours(), "#", ".");
    }

    @Scheduled(fixedRate = 1_000)
    void expireTimedOutGames() {
        Instant now = clock.instant();
        for (GameSession session : store.sessions()) if (session.status() == GameStatus.ACTIVE) {
            synchronized (session) { expireIfNecessary(session, now); }
        }
    }

    @Scheduled(fixedRateString = "${game.full-reset-milliseconds:172800000}")
    void clearAllTemporaryData() {
        store.clearAll();
        nextFullResetAt = clock.instant().plusMillis(properties.fullResetMilliseconds());
    }

    private void requireActive(GameSession session) {
        if (session.status() != GameStatus.ACTIVE) throw new GameNotActiveException(session.status());
    }

    private GameSession requireSession(UUID id) {
        return store.find(id).orElseThrow(() -> new GameNotFoundException(id));
    }

    private boolean expireIfNecessary(GameSession session, Instant now) {
        if (session.status() == GameStatus.ACTIVE && !now.isBefore(session.endsAt())) {
            session.expire(now);
            return true;
        }
        return false;
    }

    private int collectNearbyStars(GameSession session, Position center) {
        List<Position> nearby = session.remainingStars().stream()
                .filter(star -> manhattan(star, center) <= 3).toList();
        nearby.forEach(session::collectStarAt);
        return nearby.size();
    }

    private boolean handleCollisions(GameSession session, Instant now, List<String> events) {
        boolean penalized = false;
        for (EnemyState enemy : session.enemies()) {
            if (!enemy.position().equals(session.playerPosition())) continue;
            enemy.resetToSpawn();
            if (session.isProtected(now)) {
                events.add("PROTECTION_BLOCKED");
            } else if (session.consumeShield()) {
                events.add("SHIELD_BLOCKED");
            } else if (session.consumeLionShield()) {
                events.add("LION_SCARE_USED");
            } else {
                session.applyEnemyHit(now);
                events.add("ENEMY_TIME_PENALTY");
                penalized = true;
            }
        }
        return penalized;
    }

    private void moveEnemies(GameSession session) {
        Set<Position> occupied = new LinkedHashSet<>(session.enemyPositions());
        for (EnemyState enemy : session.enemies()) {
            occupied.remove(enemy.position());
            Position target = session.bananaDecoyPosition();
            Position next;
            if (target != null) {
                next = nextStepToward(session.maze(), enemy.position(), target, occupied, session);
                enemy.stopChase();
            } else if (session.hiddenInBush() || distanceFromStart(session.maze(), session.playerPosition()) <= SAFE_ZONE_DISTANCE) {
                enemy.stopChase();
                next = patrolStep(session, enemy, occupied);
            } else {
                int distance = shortestPathDistance(session.maze(), enemy.position(), session.playerPosition(), session);
                if (distance <= CHASE_DISTANCE && !enemy.chasing()) enemy.beginChase();
                if (enemy.chasing()) {
                    next = nextStepToward(session.maze(), enemy.position(), session.playerPosition(), occupied, session);
                    enemy.advanceChase();
                } else {
                    next = patrolStep(session, enemy, occupied);
                }
            }
            enemy.moveTo(next);
            occupied.add(next);
        }
        session.advanceBananaDecoy();
    }

    private Position patrolStep(GameSession session, EnemyState enemy, Set<Position> occupied) {
        List<Position> choices = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            Position next = enemy.position().move(direction);
            if (!session.maze().isWall(next) && !occupied.contains(next)
                    && distanceFromStart(session.maze(), next) > SAFE_ZONE_DISTANCE
                    && !(session.hiddenInBush() && next.equals(session.playerPosition()))) choices.add(next);
        }
        if (choices.isEmpty()) return enemy.position();
        choices.sort(positionOrder());
        long seed = session.id().getMostSignificantBits() ^ enemy.id().getLeastSignificantBits() ^ session.movesMade();
        return choices.get(new Random(seed).nextInt(choices.size()));
    }

    private Position nextStepToward(Maze maze, Position start, Position target, Set<Position> blocked, GameSession session) {
        Queue<Position> queue = new ArrayDeque<>();
        Map<Position, Position> previous = new HashMap<>();
        queue.add(start);
        previous.put(start, null);
        while (!queue.isEmpty()) {
            Position current = queue.remove();
            if (current.equals(target)) break;
            for (Direction direction : Direction.values()) {
                Position next = current.move(direction);
                if ((!maze.isWall(next) || session.canPassGate(next)) && !blocked.contains(next)
                        && distanceFromStart(maze, next) > SAFE_ZONE_DISTANCE && !previous.containsKey(next)) {
                    previous.put(next, current);
                    queue.add(next);
                }
            }
        }
        if (!previous.containsKey(target)) return start;
        Position step = target;
        while (previous.get(step) != null && !previous.get(step).equals(start)) step = previous.get(step);
        return step;
    }

    private int shortestPathDistance(Maze maze, Position start, Position target, GameSession session) {
        Queue<Position> queue = new ArrayDeque<>();
        Map<Position, Integer> distance = new HashMap<>();
        queue.add(start);
        distance.put(start, 0);
        while (!queue.isEmpty()) {
            Position current = queue.remove();
            if (current.equals(target)) return distance.get(current);
            for (Direction direction : Direction.values()) {
                Position next = current.move(direction);
                if ((!maze.isWall(next) || session.canPassGate(next)) && !distance.containsKey(next)) {
                    distance.put(next, distance.get(current) + 1);
                    queue.add(next);
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    private int distanceFromStart(Maze maze, Position target) {
        Queue<Position> queue = new ArrayDeque<>();
        Map<Position, Integer> distance = new HashMap<>();
        queue.add(maze.start());
        distance.put(maze.start(), 0);
        while (!queue.isEmpty()) {
            Position current = queue.remove();
            if (current.equals(target)) return distance.get(current);
            for (Direction direction : Direction.values()) {
                Position next = current.move(direction);
                if (!maze.isWall(next) && !distance.containsKey(next)) {
                    distance.put(next, distance.get(current) + 1);
                    queue.add(next);
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    private void recordWin(GameSession session) {
        int seconds = Math.toIntExact(Duration.between(session.startedAt(), session.finishedAt()).toSeconds());
        store.saveScoreIfAbsent(new LeaderboardEntry(session.id(), session.nickname(), session.selectedAnimal(),
                session.difficulty(), session.mode(), session.challengeDate(), session.score(), session.rescuedAnimals(),
                session.collectedStars(), seconds, session.finishedAt()));
        if (store.scoreCount() > properties.maximumLeaderboardEntries()) store.trimScores(properties.maximumLeaderboardEntries());
    }

    private GameStateResponse toResponse(GameSession session, MoveOutcome outcome, List<String> events, Instant now) {
        List<Position> stars = session.remainingStars().stream().sorted(positionOrder()).toList();
        List<RescueTarget> rescues = session.remainingRescueTargets().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(positionOrder()))
                .map(entry -> new RescueTarget(entry.getValue(), entry.getKey())).toList();
        List<PowerUp> powerUps = session.remainingPowerUps().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(positionOrder()))
                .map(entry -> new PowerUp(entry.getValue(), entry.getKey())).toList();
        List<Position> enemies = session.enemyPositions().stream().sorted(positionOrder()).toList();
        Integer nearestEnemy = enemies.stream().mapToInt(enemy -> shortestPathDistance(session.maze(), enemy,
                session.playerPosition(), session)).min().stream().boxed().findFirst().orElse(null);
        if (nearestEnemy != null && nearestEnemy == Integer.MAX_VALUE) nearestEnemy = null;
        return new GameStateResponse(session.id(), session.nickname(), session.selectedAnimal(), session.difficulty(),
                session.mode(), session.challengeDate(), session.status(), outcome, session.maze().encodedRows(),
                session.playerPosition(), session.maze().home(), stars, rescues, powerUps, enemies,
                session.bushPositions().stream().sorted(positionOrder()).toList(), session.keyPosition(), session.gatePosition(),
                session.keyCollected(), session.bananaDecoyPosition(), session.bananaCount(), session.shieldCharges(),
                session.protectionSecondsRemaining(now), session.abilityReady(), session.abilityCooldownMovesRemaining(),
                session.rescuedFollowers(), nearestEnemy != null && nearestEnemy <= WARNING_DISTANCE, nearestEnemy,
                session.collectedStars(), session.totalStars(), session.rescuedAnimals(), session.totalAnimals(),
                session.score(), session.comboMultiplier(), session.wallVisionMovesRemaining(), session.speedBoostMovesRemaining(),
                session.enemyHits(), nearestObjective(session), List.copyOf(events), secondsRemaining(session, now),
                session.movesMade(), session.startedAt(), session.endsAt());
    }

    private Position nearestObjective(GameSession session) {
        if (session.selectedAnimal() != AnimalType.PUPPY && session.wallVisionMovesRemaining() <= 0) return null;
        List<Position> objectives = new ArrayList<>(session.remainingStars());
        objectives.addAll(session.remainingRescueTargets().keySet());
        return objectives.stream().min(Comparator.comparingInt(position -> manhattan(position, session.playerPosition()))).orElse(null);
    }

    private GameExtras createExtras(Maze maze, Difficulty difficulty, long seed) {
        Set<Position> reserved = new LinkedHashSet<>(maze.stars());
        reserved.addAll(maze.rescueTargets().keySet());
        reserved.add(maze.start());
        reserved.add(maze.home());
        List<Position> available = new ArrayList<>();
        for (int row = 1; row < maze.rows() - 1; row++) for (int column = 1; column < maze.columns() - 1; column++) {
            Position position = new Position(row, column);
            if (!maze.isWall(position) && !reserved.contains(position) && distanceFromStart(maze, position) > SAFE_ZONE_DISTANCE) available.add(position);
        }
        Collections.shuffle(available, new Random(seed ^ 0xC0FFEE1234L));
        int cursor = 0;
        Set<Position> enemies = takePositions(available, cursor, difficulty.enemies());
        cursor += enemies.size();
        Map<Position, PowerUpType> powerUps = new LinkedHashMap<>();
        PowerUpType[] types = PowerUpType.values();
        for (int index = 0; index < difficulty.powerUps() && cursor < available.size(); index++, cursor++)
            powerUps.put(available.get(cursor), types[index % types.length]);
        int bushCount = difficulty == Difficulty.EASY ? 2 : difficulty == Difficulty.MEDIUM ? 4 : 6;
        Set<Position> bushes = takePositions(available, cursor, bushCount);
        cursor += bushes.size();
        Position gate = findShortcutGate(maze, seed);
        Position key = gate != null && cursor < available.size() ? available.get(cursor) : null;
        return new GameExtras(powerUps, enemies, bushes, key, gate);
    }

    private Set<Position> takePositions(List<Position> positions, int start, int count) {
        Set<Position> result = new LinkedHashSet<>();
        for (int i = start; i < positions.size() && i < start + count; i++) result.add(positions.get(i));
        return result;
    }

    private Position findShortcutGate(Maze maze, long seed) {
        List<Position> candidates = new ArrayList<>();
        for (int row = 1; row < maze.rows() - 1; row++) for (int column = 1; column < maze.columns() - 1; column++) {
            Position wall = new Position(row, column);
            if (!maze.isWall(wall)) continue;
            boolean horizontal = !maze.isWall(new Position(row, column - 1)) && !maze.isWall(new Position(row, column + 1));
            boolean vertical = !maze.isWall(new Position(row - 1, column)) && !maze.isWall(new Position(row + 1, column));
            if (horizontal ^ vertical) candidates.add(wall);
        }
        if (candidates.isEmpty()) return null;
        candidates.sort(positionOrder());
        return candidates.get(new Random(seed ^ 0x6A7E1234L).nextInt(candidates.size()));
    }

    private long dailySeed(LocalDate date, Difficulty difficulty) { return date.toEpochDay() * 31L + difficulty.ordinal() * 1_000_003L; }

    private long secondsRemaining(GameSession session, Instant now) {
        if (session.status() != GameStatus.ACTIVE) return 0;
        long milliseconds = Math.max(0, Duration.between(now, session.endsAt()).toMillis());
        return (milliseconds + 999) / 1_000;
    }

    private int manhattan(Position a, Position b) { return Math.abs(a.row() - b.row()) + Math.abs(a.column() - b.column()); }
    private Comparator<Position> positionOrder() { return Comparator.comparingInt(Position::row).thenComparingInt(Position::column); }
    private String normalizeNickname(String nickname) { return nickname.trim().replaceAll("\\s+", " "); }

    private record GameExtras(Map<Position, PowerUpType> powerUps, Set<Position> enemies,
                              Set<Position> bushes, Position keyPosition, Position gatePosition) { }
}
