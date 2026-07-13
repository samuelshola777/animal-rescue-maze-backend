package com.samuel.animalrescue.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "game")
public record GameProperties(
        int rows,
        int columns,
        int starCount,
        int rescueCount,
        int durationSeconds,
        int maximumSessions,
        int maximumLeaderboardEntries,
        long fullResetMilliseconds
) {
    public GameProperties {
        requireOddAtLeast(rows, 9, "rows");
        requireOddAtLeast(columns, 9, "columns");
        if (starCount < 1) throw new IllegalArgumentException("starCount must be positive");
        if (rescueCount < 1) throw new IllegalArgumentException("rescueCount must be positive");
        if (durationSeconds < 10) throw new IllegalArgumentException("durationSeconds must be at least 10");
        if (maximumSessions < 1) throw new IllegalArgumentException("maximumSessions must be positive");
        if (maximumLeaderboardEntries < 1) {
            throw new IllegalArgumentException("maximumLeaderboardEntries must be positive");
        }
        if (fullResetMilliseconds < 60_000) {
            throw new IllegalArgumentException("fullResetMilliseconds must be at least one minute");
        }
    }

    private static void requireOddAtLeast(int value, int minimum, String name) {
        if (value < minimum || value % 2 == 0) {
            throw new IllegalArgumentException(name + " must be odd and at least " + minimum);
        }
    }
}
