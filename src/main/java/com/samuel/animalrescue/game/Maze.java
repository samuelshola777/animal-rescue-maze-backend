package com.samuel.animalrescue.game;

import com.samuel.animalrescue.model.AnimalType;
import com.samuel.animalrescue.model.Position;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class Maze {
    private final boolean[][] walls;
    private final Position start;
    private final Position home;
    private final Set<Position> stars;
    private final Map<Position, AnimalType> rescueTargets;

    public Maze(boolean[][] walls,
                Position start,
                Position home,
                Set<Position> stars,
                Map<Position, AnimalType> rescueTargets) {
        this.walls = copy(walls);
        this.start = start;
        this.home = home;
        this.stars = Collections.unmodifiableSet(new LinkedHashSet<>(stars));
        this.rescueTargets = Collections.unmodifiableMap(new LinkedHashMap<>(rescueTargets));
    }

    public int rows() {
        return walls.length;
    }

    public int columns() {
        return walls[0].length;
    }

    public boolean isWall(Position position) {
        return !isInside(position) || walls[position.row()][position.column()];
    }

    public boolean isInside(Position position) {
        return position.row() >= 0 && position.row() < rows()
                && position.column() >= 0 && position.column() < columns();
    }

    public Position start() {
        return start;
    }

    public Position home() {
        return home;
    }

    public Set<Position> stars() {
        return stars;
    }

    public Map<Position, AnimalType> rescueTargets() {
        return rescueTargets;
    }

    public java.util.List<String> encodedRows() {
        java.util.List<String> result = new java.util.ArrayList<>(rows());
        for (int row = 0; row < rows(); row++) {
            StringBuilder encoded = new StringBuilder(columns());
            for (int column = 0; column < columns(); column++) {
                encoded.append(walls[row][column] ? '#' : '.');
            }
            result.add(encoded.toString());
        }
        return java.util.List.copyOf(result);
    }

    private static boolean[][] copy(boolean[][] source) {
        boolean[][] copy = new boolean[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }
}
