package game.core.game;

import game.core.ecs.Entity;
import game.core.ecs.components.Position;
import game.core.ecs.components.Stats;
import game.core.ecs.systems.AiDecisionSystem;
import game.core.ecs.systems.AiMovementSystem;
import game.core.ecs.systems.AiPerceptionSystem;
import game.core.ecs.systems.CombatSystem;
import game.core.ecs.systems.InteractionSystem;
import game.core.ecs.systems.ItemSystem;
import game.core.ecs.systems.TurretSystem;
import game.core.map.Fov;
import game.util.Rng;

import java.awt.Point;
import java.util.List;

public class TurnEngine {
    private final GameState gameState;
    private final CombatSystem combatSystem;
    private final TurretSystem turretSystem;
    private final ItemSystem itemSystem;
    private final InteractionSystem interactionSystem;
    private final AiPerceptionSystem aiPerceptionSystem;
    private final AiDecisionSystem aiDecisionSystem;
    private final AiMovementSystem aiMovementSystem;

    public TurnEngine(GameState gameState, Rng rng) {
        this.gameState = gameState;
        this.combatSystem = new CombatSystem(rng);
        this.turretSystem = new TurretSystem(combatSystem);
        this.itemSystem = new ItemSystem();
        this.interactionSystem = new InteractionSystem();
        this.aiPerceptionSystem = new AiPerceptionSystem();
        this.aiDecisionSystem = new AiDecisionSystem();
        this.aiMovementSystem = new AiMovementSystem(rng);
    }

    public void processTurn() {
        // Player action (move, peek, etc.) is handled by other methods before this.
        // This method processes the consequences of the player's action.

        // 1. Turrets act
        turretSystem.process(gameState);
        // 2. Drones act
        aiPerceptionSystem.process(gameState);
        aiDecisionSystem.process(gameState);
        aiMovementSystem.process(gameState);
        // 3. Update player's view for the next render
        updateFov();
    }

    public boolean handleMove(Direction direction) {
        if (!gameState.player.has(Position.class)) return false;
        Position playerPos = gameState.player.get(Position.class).get();
        int newX = playerPos.x() + direction.dx;
        int newY = playerPos.y() + direction.dy;

        if (!gameState.map.isInBounds(newX, newY)) {
            return false; // Can't move out of bounds. No turn taken.
        }

        Entity targetEntity = getEntityAt(newX, newY);
        if (targetEntity != null && targetEntity != gameState.player && targetEntity.has(Stats.class)) {
            combatSystem.handleAttack(gameState.player, targetEntity);
            return true; // Attack takes a turn.
        }

        if (gameState.map.getTile(newX, newY).isWalkable()) {
            gameState.player.add(new Position(newX, newY));
            return true; // Movement takes a turn.
        }

        // It's a wall or other unwalkable tile.
        combatSystem.combatLogs.add("You bump into the wall.");
        return false; // Bumping a wall doesn't take a turn.
    }

    public boolean handleUseItem(String itemName, Point target) {
        if ("med-gel".equalsIgnoreCase(itemName)) {
            return itemSystem.useMedGel(gameState);
        }
        if ("emp-charge".equalsIgnoreCase(itemName)) {
            if (target == null) {
                gameState.messageLog.add("You must select a target for the EMP charge.");
                return false;
            }
            return itemSystem.useEmpCharge(gameState, target);
        }
        return false;
    }

    public boolean handleInteract() {
        return interactionSystem.handleInteraction(gameState, this);
    }

    public Entity getEntityAt(int x, int y) {
        for (Entity entity : gameState.entities) {
            if (entity.get(Position.class).map(p -> p.x() == x && p.y() == y).orElse(false)) {
                return entity;
            }
        }
        return null;
    }

    public void generateNoise(Point location, int radius) {
        gameState.noiseEvents.add(new NoiseEvent(location, radius));
    }

    public void updateFov() {
        gameState.player.get(Position.class).ifPresent(pos -> {
            boolean[][] fov = Fov.computeFov(gameState.map, pos.x(), pos.y(), 8);
            gameState.updateVisibility(fov);
        });
    }

    public void handlePeek(Direction direction) {
        gameState.player.get(Position.class).ifPresent(pos -> {
            int x0 = pos.x();
            int y0 = pos.y();
            // Calculate the end of the peek ray, 6 tiles away
            int x1 = x0 + direction.dx * 6;
            int y1 = y0 + direction.dy * 6;

            List<Point> peekLine = game.util.Pathfinder.bresenhamLine(x0, y0, x1, y1);
            for (Point p : peekLine) {
                if (gameState.map.isInBounds(p.x, p.y)) {
                    gameState.visibleTiles[p.x][p.y] = true;
                    // Also mark as explored
                    gameState.exploredTiles[p.x][p.y] = true;
                    if (!gameState.map.getTile(p.x, p.y).isTransparent()) {
                        break;
                    }
                } else {
                    break;
                }
            }
            // This action will advance the turn, which is handled in the main game loop.
        });
    }
}
