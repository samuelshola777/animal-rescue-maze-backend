package com.samuel.animalrescue.game;

import com.samuel.animalrescue.model.Direction;
import com.samuel.animalrescue.model.Position;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MazeGeneratorTest {
    private final MazeGenerator generator = new MazeGenerator();

    @Test
    void generatesReachableMazeWithRequestedCollectibles() {
        Maze maze = generator.generate(15, 15, 10, 3, 12345L);

        Set<Position> reachable = reachableFrom(maze, maze.start());

        assertThat(reachable).contains(maze.home());
        assertThat(reachable).containsAll(maze.stars());
        assertThat(reachable).containsAll(maze.rescueTargets().keySet());
        assertThat(maze.stars()).hasSize(10);
        assertThat(maze.rescueTargets()).hasSize(3);
        assertThat(maze.encodedRows()).hasSize(15).allSatisfy(row -> assertThat(row).hasSize(15));
    }

    @Test
    void producesTheSameMazeForTheSameSeed() {
        Maze first = generator.generate(15, 15, 10, 3, 99L);
        Maze second = generator.generate(15, 15, 10, 3, 99L);

        assertThat(first.encodedRows()).isEqualTo(second.encodedRows());
        assertThat(first.stars()).isEqualTo(second.stars());
        assertThat(first.rescueTargets()).isEqualTo(second.rescueTargets());
    }

    private Set<Position> reachableFrom(Maze maze, Position start) {
        Set<Position> visited = new HashSet<>();
        Queue<Position> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            Position current = queue.remove();
            for (Direction direction : Direction.values()) {
                Position next = current.move(direction);
                if (!maze.isWall(next) && visited.add(next)) queue.add(next);
            }
        }
        return visited;
    }
}
