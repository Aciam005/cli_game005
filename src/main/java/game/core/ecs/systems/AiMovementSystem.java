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

            switch (ai.state) {
                case PATROL -> handlePatrolMovement(gameState, entity, pos);
                case CAMP_VENT -> { /* Do nothing */ }
                case CHASE, INVESTIGATE, SEARCH -> handleTargetedMovement(gameState, entity, ai, pos);
            }
        }
    }

    private void handlePatrolMovement(GameState gameState, Entity entity, Position pos) {
        List<Point> neighbors = Pathfinder.getAiNeighbors(gameState.map, new Point(pos.x(), pos.y()));
        neighbors.removeIf(p -> gameState.getEntityAt(p.x, p.y) != null);
        if (!neighbors.isEmpty()) {
            Point nextMove = neighbors.get(rng.nextInt(neighbors.size()));
            entity.add(new Position(nextMove.x, nextMove.y));
        }
    }

    private void handleTargetedMovement(GameState gameState, Entity entity, AI ai, Position pos) {
        if (ai.targetPosition == null) return;

        if (ai.state != AiState.CHASE && pos.x() == ai.targetPosition.x && pos.y() == ai.targetPosition.y) {
            ai.currentPath = null;
            return;
        }

        if (ai.currentPath == null || ai.currentPath.isEmpty() || ai.state == AiState.CHASE) {
            ai.currentPath = Pathfinder.findAiPath(gameState.map, new Point(pos.x(), pos.y()), ai.targetPosition);
        }

        if (ai.currentPath != null && !ai.currentPath.isEmpty()) {
            Point nextMove = ai.currentPath.get(0);
            Entity targetEntity = gameState.getEntityAt(nextMove.x, nextMove.y);

            if (targetEntity != null && targetEntity.get(Flags.class).map(f -> f.isPlayer).orElse(false)) {
                combatSystem.handleAttack(gameState, entity, targetEntity);
                ai.currentPath = null;
            } else if (targetEntity == null && gameState.map.getTile(nextMove.x, nextMove.y).isWalkable()) {
                entity.add(new Position(nextMove.x, nextMove.y));
                ai.currentPath.remove(0);
            } else {
                ai.currentPath = null;
            }
        }
    }
}
