package com.samuel.animalrescue.game;

import com.samuel.animalrescue.model.AnimalType;
import com.samuel.animalrescue.model.Direction;
import com.samuel.animalrescue.model.Position;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Component
public class MazeGenerator {

    public Maze generate(int rows, int columns, int starCount, int rescueCount, long seed) {
        validate(rows, columns, starCount, rescueCount);

        boolean[][] walls = new boolean[rows][columns];
        for (boolean[] row : walls) Arrays.fill(row, true);

        Position start = new Position(1, 1);
        Position home = new Position(rows - 2, columns - 2);
        carveMaze(walls, start, new Random(seed));

        List<Position> available = availablePaths(walls, start, home);
        if (available.size() < starCount + rescueCount) {
            throw new IllegalArgumentException("Maze is too small for the requested collectibles");
        }
        java.util.Collections.shuffle(available, new Random(seed ^ 0x5DEECE66DL));

        Set<Position> stars = new LinkedHashSet<>(available.subList(0, starCount));
        Map<Position, AnimalType> rescues = new LinkedHashMap<>();
        AnimalType[] animals = AnimalType.values();
        for (int index = 0; index < rescueCount; index++) {
            Position position = available.get(starCount + index);
            rescues.put(position, animals[index % animals.length]);
        }

        return new Maze(walls, start, home, stars, rescues);
    }

    private void carveMaze(boolean[][] walls, Position start, Random random) {
        Deque<Position> stack = new ArrayDeque<>();
        walls[start.row()][start.column()] = false;
        stack.push(start);

        while (!stack.isEmpty()) {
            Position current = stack.peek();
            List<Position> candidates = unvisitedTwoStepsAway(walls, current);
            if (candidates.isEmpty()) {
                stack.pop();
                continue;
            }

            Position next = candidates.get(random.nextInt(candidates.size()));
            int middleRow = (current.row() + next.row()) / 2;
            int middleColumn = (current.column() + next.column()) / 2;
            walls[middleRow][middleColumn] = false;
            walls[next.row()][next.column()] = false;
            stack.push(next);
        }
    }

    private List<Position> unvisitedTwoStepsAway(boolean[][] walls, Position current) {
        List<Position> candidates = new ArrayList<>(4);
        for (Direction direction : Direction.values()) {
            Position candidate = new Position(
                    current.row() + direction.rowDelta() * 2,
                    current.column() + direction.columnDelta() * 2
            );
            if (candidate.row() > 0 && candidate.row() < walls.length - 1
                    && candidate.column() > 0 && candidate.column() < walls[0].length - 1
                    && walls[candidate.row()][candidate.column()]) {
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    private List<Position> availablePaths(boolean[][] walls, Position start, Position home) {
        List<Position> available = new ArrayList<>();
        for (int row = 1; row < walls.length - 1; row++) {
            for (int column = 1; column < walls[0].length - 1; column++) {
                Position position = new Position(row, column);
                if (!walls[row][column] && !position.equals(start) && !position.equals(home)) {
                    available.add(position);
                }
            }
        }
        return available;
    }

    private void validate(int rows, int columns, int starCount, int rescueCount) {
        if (rows < 5 || columns < 5 || rows % 2 == 0 || columns % 2 == 0) {
            throw new IllegalArgumentException("Maze dimensions must be odd and at least 5");
        }
        if (starCount < 0 || rescueCount < 0) {
            throw new IllegalArgumentException("Collectible counts cannot be negative");
        }
    }
}
