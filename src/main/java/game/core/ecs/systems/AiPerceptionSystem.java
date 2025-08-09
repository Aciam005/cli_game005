package game.core.ecs.systems;

import game.core.ecs.Entity;
import game.core.ecs.components.AI;
import game.core.ecs.components.AiPerception;
import game.core.ecs.components.Position;
import game.core.game.GameState;
import game.core.game.NoiseEvent;
import game.util.Pathfinder;

import java.awt.Point;
import java.util.List;

public class AiPerceptionSystem {

    public void process(GameState gameState) {
        List<NoiseEvent> currentNoiseEvents = List.copyOf(gameState.noiseEvents);

        for (Entity entity : gameState.entities) {
            if (entity.has(AI.class)) {
                AiPerception perception = new AiPerception();
                Position aiPos = entity.get(Position.class).orElse(null);
                Position playerPos = gameState.player.get(Position.class).orElse(null);

                if (aiPos == null || playerPos == null) {
                    entity.add(perception);
                    continue;
                }

                // Vision check
                if (new Point(aiPos.x(), aiPos.y()).distance(playerPos.x(), playerPos.y()) <= 10) { // Vision range
                    List<Point> lineToPlayer = Pathfinder.bresenhamLine(aiPos.x(), aiPos.y(), playerPos.x(), playerPos.y());
                    boolean hasLos = true;
                    for (Point p : lineToPlayer) {
                        if (!gameState.map.getTile(p.x, p.y).isTransparent() && !(p.x == playerPos.x() && p.y == playerPos.y())) {
                            hasLos = false;
                            break;
                        }
                    }
                    if (hasLos) {
                        perception.canSeePlayer = true;
                        perception.lastKnownPlayerPosition = new Point(playerPos.x(), playerPos.y());
                    }
                }

                // Hearing check
                for (NoiseEvent event : currentNoiseEvents) {
                    if (new Point(aiPos.x(), aiPos.y()).distance(event.location()) <= event.radius()) {
                        perception.noiseLocation = event.location();
                        break;
                    }
                }

                entity.add(perception);
            }
        }
    }
}
