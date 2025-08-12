package game.core.ecs.systems;

import game.core.ecs.Entity;
import game.core.ecs.components.AI;
import game.core.ecs.components.AiState;
import game.core.ecs.components.Position;
import game.core.game.GameState;
import game.util.Pathfinder;
import game.util.Rng;
import java.awt.Point;
import java.util.List;

public class AiMovementSystem {

    private final Rng rng;

    public AiMovementSystem(Rng rng) {
        this.rng = rng;
    }

    public void process(GameState gameState) {
        for (Entity entity : gameState.entities) {
            if (!entity.has(AI.class) || !entity.has(Position.class)) {
                continue;
            }

            AI ai = entity.get(AI.class).get();
            Position pos = entity.get(Position.class).get();

            if (ai.state == AiState.PATROL) {
                // Simple random walk, avoiding vents
                List<Point> neighbors = Pathfinder.getAiNeighbors(gameState.map, new Point(pos.x(), pos.y()));
                if (!neighbors.isEmpty()) {
                    Point nextMove = neighbors.get(rng.nextInt(neighbors.size()));
                    entity.add(new Position(nextMove.x, nextMove.y));
                }
            } else if (ai.state == AiState.CAMP_VENT) {
                // Do not move while camping
            } else if (ai.targetPosition != null) {
                // Don't move if already at target
                if (pos.x() == ai.targetPosition.x && pos.y() == ai.targetPosition.y) {
                    ai.currentPath = null; // Clear path
                    continue;
                }

                // Recalculate path if it's null or we're in CHASE mode
                if (ai.currentPath == null || ai.currentPath.isEmpty() || ai.state == AiState.CHASE) {
                    ai.currentPath = Pathfinder.findAiPath(gameState.map, new Point(pos.x(), pos.y()), ai.targetPosition);
                }

                if (ai.currentPath != null && !ai.currentPath.isEmpty()) {
                    Point nextMove = ai.currentPath.remove(0);
                    entity.add(new Position(nextMove.x, nextMove.y));
                }
            }
        }
    }
}
