package game.core.ecs.systems;

import game.core.ecs.Entity;
import game.core.ecs.components.AI;
import game.core.ecs.components.AiPerception;
import game.core.ecs.components.AiState;
import game.core.ecs.components.Position;
import game.core.game.GameState;
import game.core.map.Tile;
import game.util.Config;

import java.awt.Point;

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
                case PATROL -> handlePatrolState(ai, perception);
                case INVESTIGATE -> handleInvestigateState(ai, perception, pos);
                case CHASE -> handleChaseState(gameState, ai, perception);
                case SEARCH -> handleSearchState(ai, perception, pos);
                case CAMP_VENT -> handleCampVentState(ai, perception);
            }
        }
        gameState.noiseEvents.clear();
    }

    private void handlePatrolState(AI ai, AiPerception perception) {
        if (perception.canSeePlayer) {
            ai.state = AiState.CHASE;
            ai.targetPosition = perception.lastKnownPlayerPosition;
        } else if (perception.noiseLocation != null) {
            ai.state = AiState.INVESTIGATE;
            ai.targetPosition = perception.noiseLocation;
        }
    }

    private void handleInvestigateState(AI ai, AiPerception perception, Position pos) {
        if (perception.canSeePlayer) {
            ai.state = AiState.CHASE;
            ai.targetPosition = perception.lastKnownPlayerPosition;
        } else if (pos.x() == ai.targetPosition.x && pos.y() == ai.targetPosition.y) {
            // Reached noise/investigation spot, now search the area for a few turns.
            ai.state = AiState.SEARCH;
            ai.searchTurnsLeft = Config.getInt("ai.search_turns");
        }
    }

    private void handleChaseState(GameState gameState, AI ai, AiPerception perception) {
        if (perception.canSeePlayer) {
            ai.targetPosition = perception.lastKnownPlayerPosition;
        } else {
            Point lastPos = perception.lastKnownPlayerPosition;
            Point ventExit = findAdjacentVent(gameState, lastPos);
            if (ventExit != null) {
                // Player disappeared near a vent, so camp it for a few turns.
                ai.state = AiState.CAMP_VENT;
                ai.targetPosition = ventExit;
                ai.campTurnsLeft = Config.getInt("ai.camp_turns");
                ai.currentPath = null;
            } else {
                // Player disappeared, but not near a vent, so search the area.
                ai.state = AiState.SEARCH;
                ai.searchTurnsLeft = Config.getInt("ai.search_turns");
            }
        }
    }

    private void handleSearchState(AI ai, AiPerception perception, Position pos) {
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
    }

    private void handleCampVentState(AI ai, AiPerception perception) {
        if (perception.canSeePlayer) {
            ai.state = AiState.CHASE;
            ai.targetPosition = perception.lastKnownPlayerPosition;
            ai.campTurnsLeft = 0;
        } else {
            ai.campTurnsLeft--;
            if (ai.campTurnsLeft <= 0) {
                ai.state = AiState.PATROL;
                ai.targetPosition = null;
            }
        }
    }

    private Point findAdjacentVent(GameState gameState, Point position) {
        if (position == null) return null;
        Point[] neighbors = {
                new Point(position.x, position.y - 1), // North
                new Point(position.x, position.y + 1), // South
                new Point(position.x - 1, position.y), // West
                new Point(position.x + 1, position.y)  // East
        };
        for (Point p : neighbors) {
            Tile tile = gameState.map.getTile(p.x, p.y);
            if (tile != null && tile.isVent()) {
                return p;
            }
        }
        return null;
    }
}
