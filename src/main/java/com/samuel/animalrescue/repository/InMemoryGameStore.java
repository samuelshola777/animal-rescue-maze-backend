package com.samuel.animalrescue.repository;

import com.samuel.animalrescue.model.GameSession;
import com.samuel.animalrescue.model.LeaderboardEntry;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.samuel.animalrescue.model.Difficulty;
import com.samuel.animalrescue.model.GameMode;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryGameStore {
    private static final Comparator<LeaderboardEntry> LEADERBOARD_ORDER =
            Comparator.comparingInt(LeaderboardEntry::score).reversed()
                    .thenComparingInt(LeaderboardEntry::completionSeconds)
                    .thenComparing(LeaderboardEntry::achievedAt);

    private final ConcurrentMap<UUID, GameSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, LeaderboardEntry> scores = new ConcurrentHashMap<>();

    public void save(GameSession session) {
        sessions.put(session.id(), session);
    }

    public Optional<GameSession> find(UUID id) {
        return Optional.ofNullable(sessions.get(id));
    }

    public boolean remove(UUID id) {
        return sessions.remove(id) != null;
    }

    public int sessionCount() {
        return sessions.size();
    }

    public List<GameSession> sessions() {
        return List.copyOf(sessions.values());
    }

    public void saveScoreIfAbsent(LeaderboardEntry entry) {
        scores.putIfAbsent(entry.gameId(), entry);
    }

    public List<LeaderboardEntry> leaderboard(int limit) {
        return leaderboard(limit, null, null, null);
    }

    public List<LeaderboardEntry> leaderboard(int limit,
                                              GameMode mode,
                                              Difficulty difficulty,
                                              LocalDate challengeDate) {
        return scores.values().stream()
                .filter(entry -> mode == null || entry.mode() == mode)
                .filter(entry -> difficulty == null || entry.difficulty() == difficulty)
                .filter(entry -> challengeDate == null || challengeDate.equals(entry.challengeDate()))
                .sorted(LEADERBOARD_ORDER)
                .limit(limit)
                .toList();
    }

    public int scoreCount() {
        return scores.size();
    }

    public void trimScores(int maximumEntries) {
        List<UUID> toRemove = scores.values().stream()
                .sorted(LEADERBOARD_ORDER)
                .skip(maximumEntries)
                .map(LeaderboardEntry::gameId)
                .toList();
        toRemove.forEach(scores::remove);
    }

    public void clearAll() {
        sessions.clear();
        scores.clear();
    }
}
