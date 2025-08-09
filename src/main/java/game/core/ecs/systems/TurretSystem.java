package game.core.ecs.systems;

import game.core.ecs.Entity;
import game.core.ecs.components.Flags;
import game.core.ecs.components.Position;
import game.core.game.GameState;
import game.util.Pathfinder; // For LoS check

import java.awt.Point;
import java.util.List;

public class TurretSystem {

    private final CombatSystem combatSystem;

    public TurretSystem(CombatSystem combatSystem) {
        this.combatSystem = combatSystem;
    }

    public void process(GameState gameState) {
        for (Entity entity : gameState.entities) {
            if (entity.get(Flags.class).map(f -> f.isTurret).orElse(false)) {
                runTurretLogic(entity, gameState);
            }
        }
    }

    private void runTurretLogic(Entity turret, GameState gameState) {
        Flags flags = turret.get(Flags.class).get();

        // Handle disabled state
        if (flags.isDisabled) {
            flags.disabledTurns--;
            if (flags.disabledTurns <= 0) {
                flags.isDisabled = false;
                combatSystem.combatLogs.add("A turret powers back on!");
            }
            return; // Do nothing else if disabled
        }

        // Firing logic requires player and turret to have positions
        if (!turret.has(Position.class) || !gameState.player.has(Position.class)) {
            return;
        }

        Position turretPos = turret.get(Position.class).get();
        Position playerPos = gameState.player.get(Position.class).get();

        // Check distance first for performance
        if (new Point(turretPos.x(), turretPos.y()).distance(playerPos.x(), playerPos.y()) > 6) {
            return; // Player is out of range
        }

        // Check Line of Sight (LoS)
        List<Point> lineToPlayer = Pathfinder.bresenhamLine(turretPos.x(), turretPos.y(), playerPos.x(), playerPos.y());
        boolean canSeePlayer = true;
        for (Point p : lineToPlayer) {
            if (!gameState.map.getTile(p.x, p.y).isTransparent() && !(p.x == playerPos.x() && p.y == playerPos.y())) {
                canSeePlayer = false;
                break;
            }
        }

        if (canSeePlayer) {
            combatSystem.handleAttack(turret, gameState.player);
        }
    }
}
