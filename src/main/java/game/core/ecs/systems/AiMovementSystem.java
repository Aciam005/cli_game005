package game.core.ecs.systems;

import game.core.ecs.Entity;
import game.core.ecs.components.AI;
import game.core.ecs.components.AiState;
import game.core.ecs.components.Flags;
import game.core.ecs.components.Position;
import game.core.game.GameState;
import game.util.Pathfinder;
import game.util.Rng;
import java.awt.Point;
import java.util.List;

public class AiMovementSystem {

    private final Rng rng;
    private final CombatSystem combatSystem;

    public AiMovementSystem(Rng rng, CombatSystem combatSystem) {
        this.rng = rng;
        this.combatSystem = combatSystem;
    }

    public void process(GameState gameState) {
        for (Entity entity : gameState.entities) {
            if (!entity.has(AI.class) || !entity.has(Position.class)) {
                continue;
            }

            AI ai = entity.get(AI.class).get();
            Position pos = entity.get(Position.class).get();

            if (ai.state == AiState.PATROL) {
                // Simple random walk, avoiding vents and other entities
                List<Point> neighbors = Pathfinder.getAiNeighbors(gameState.map, new Point(pos.x(), pos.y()));
                neighbors.removeIf(p -> getEntityAt(gameState, p.x, p.y) != null); // Avoid bumping into things
                if (!neighbors.isEmpty()) {
                    Point nextMove = neighbors.get(rng.nextInt(neighbors.size()));
                    entity.add(new Position(nextMove.x, nextMove.y));
                }
            } else if (ai.state == AiState.CAMP_VENT) {
                // Do not move while camping
            } else if (ai.targetPosition != null) {
                // Don't move if already at target (unless chasing)
                if (ai.state != AiState.CHASE && pos.x() == ai.targetPosition.x && pos.y() == ai.targetPosition.y) {
                    ai.currentPath = null; // Clear path
                    continue;
                }

                // Recalculate path if it's null or we're in CHASE mode (to adapt to player movement)
                if (ai.currentPath == null || ai.currentPath.isEmpty() || ai.state == AiState.CHASE) {
                    ai.currentPath = Pathfinder.findAiPath(gameState.map, new Point(pos.x(), pos.y()), ai.targetPosition);
                }

                if (ai.currentPath != null && !ai.currentPath.isEmpty()) {
                    Point nextMove = ai.currentPath.get(0); // Peek at next move
                    Entity targetEntity = getEntityAt(gameState, nextMove.x, nextMove.y);

                    // If the next move is onto the player's tile, attack instead of moving.
                    if (targetEntity != null && targetEntity.get(Flags.class).map(f -> f.isPlayer).orElse(false)) {
                        combatSystem.handleAttack(gameState, entity, targetEntity);
                        ai.currentPath = null; // Force path recalculation next turn
                    } else if (targetEntity == null && gameState.map.getTile(nextMove.x, nextMove.y).isWalkable()) {
                        // The way is clear, so move.
                        entity.add(new Position(nextMove.x, nextMove.y));
                        ai.currentPath.remove(0); // Consume the path node
                    } else {
                        // Path is blocked by something unexpected (door, another drone, etc.).
                        // Clear path to force recalculation.
                        ai.currentPath = null;
                    }
                }
            }
        }
    }

    private Entity getEntityAt(GameState gameState, int x, int y) {
        for (Entity entity : gameState.entities) {
            if (entity.get(Position.class).map(p -> p.x() == x && p.y() == y).orElse(false)) {
                return entity;
            }
        }
        return null;
    }
}
