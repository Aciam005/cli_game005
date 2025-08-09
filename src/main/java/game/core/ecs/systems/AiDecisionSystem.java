package game.core.ecs.systems;

import game.core.ecs.Entity;
import game.core.ecs.components.AI;
import game.core.ecs.components.AiPerception;
import game.core.ecs.components.AiState;
import game.core.ecs.components.Position;
import game.core.game.GameState;

public class AiDecisionSystem {

    public void process(GameState gameState) {
        for (Entity entity : gameState.entities) {
            if (!entity.has(AI.class) || !entity.has(AiPerception.class)) {
                continue;
            }

            AI ai = entity.get(AI.class).get();
            AiPerception perception = entity.get(AiPerception.class).get();
            Position pos = entity.get(Position.class).get();

            // FSM Logic
            switch (ai.state) {
                case PATROL:
                    if (perception.canSeePlayer) {
                        ai.state = AiState.CHASE;
                        ai.targetPosition = perception.lastKnownPlayerPosition;
                    } else if (perception.noiseLocation != null) {
                        ai.state = AiState.INVESTIGATE;
                        ai.targetPosition = perception.noiseLocation;
                    }
                    break;
                case INVESTIGATE:
                    if (perception.canSeePlayer) {
                        ai.state = AiState.CHASE;
                        ai.targetPosition = perception.lastKnownPlayerPosition;
                    } else if (pos.x() == ai.targetPosition.x && pos.y() == ai.targetPosition.y) {
                        // Reached investigation spot
                        ai.state = AiState.SEARCH;
                        ai.searchTurnsLeft = 5;
                    }
                    break;
                case CHASE:
                    if (perception.canSeePlayer) {
                        ai.targetPosition = perception.lastKnownPlayerPosition;
                    } else {
                        ai.state = AiState.SEARCH;
                        ai.searchTurnsLeft = 5;
                    }
                    break;
                case SEARCH:
                    if (perception.canSeePlayer) {
                        ai.state = AiState.CHASE;
                        ai.targetPosition = perception.lastKnownPlayerPosition;
                    } else {
                        ai.searchTurnsLeft--;
                        boolean atTarget = (pos.x() == ai.targetPosition.x && pos.y() == ai.targetPosition.y);
                        if (ai.searchTurnsLeft <= 0 || atTarget) {
                            ai.state = AiState.PATROL;
                            ai.targetPosition = null;
                        }
                    }
                    break;
            }
        }
        gameState.noiseEvents.clear();
    }
}
