package game.core.ecs.systems;

import game.core.ecs.Entity;
import game.core.ecs.components.Flags;
import game.core.ecs.components.Inventory;
import game.core.ecs.components.Position;
import game.core.ecs.components.Stats;
import game.core.game.Direction;
import game.core.game.GameState;
import game.core.game.NoiseEvent;
import game.core.map.Tile;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class ShootingSystem {

    public final List<String> shootingLogs = new ArrayList<>();
    public final List<Point> rayPath = new ArrayList<>();

    public boolean fire(GameState gameState, Direction direction) {
        rayPath.clear();
        shootingLogs.clear();

        Entity player = gameState.player;
        Position playerPos = player.get(Position.class).get();
        Inventory playerInv = player.get(Inventory.class).get();

        int ammo = playerInv.items.getOrDefault("ammo", 0);
        if (ammo <= 0) {
            gameState.messageLog.add("Click. Out of ammo.");
            return false; // No turn taken
        }

        // Consume ammo
        playerInv.items.put("ammo", ammo - 1);

        // Raycast
        int x = playerPos.x();
        int y = playerPos.y();

        for (int i = 1; i <= 6; i++) { // Max range 6, same as Peek
            x += direction.dx;
            y += direction.dy;
            rayPath.add(new Point(x, y));

            // Check for entity at this position
            for (Entity entity : gameState.entities) {
                if (entity == player) continue;
                if (!entity.has(Stats.class)) continue; // Can't shoot what has no health

                Position entityPos = entity.get(Position.class).orElse(null);
                if (entityPos != null && entityPos.x() == x && entityPos.y() == y) {
                    applyDamage(gameState, entity);
                    gameState.noiseEvents.add(new NoiseEvent(new Point(playerPos.x(), playerPos.y()), 12));
                    return true; // Turn taken
                }
            }

            // Check for map collision
            Tile tile = gameState.map.getTile(x, y);
            if (tile != null && !tile.isTransparent()) {
                gameState.messageLog.add("The shot hit a " + tile.name().toLowerCase().replace('_', ' ') + ".");
                gameState.noiseEvents.add(new NoiseEvent(new Point(playerPos.x(), playerPos.y()), 12));
                return true; // Turn taken, even if it's a miss
            }
        }

        gameState.messageLog.add("The shot went into the darkness.");
        gameState.noiseEvents.add(new NoiseEvent(new Point(playerPos.x(), playerPos.y()), 12));
        return true; // Turn taken
    }

    private void applyDamage(GameState gameState, Entity target) {
        Stats oldStats = target.get(Stats.class).get();
        int damage = 2;
        int newHp = oldStats.hp() - damage;

        Stats newStats = new Stats(newHp, oldStats.maxHp(), oldStats.atk(), oldStats.ev());
        target.add(newStats);

        String targetName = target.get(Flags.class).map(f -> f.isTurret ? "Turret" : "Drone").orElse("Entity");
        gameState.messageLog.add(String.format("You hit the %s for %d damage!", targetName, damage));

        if (newHp <= 0) {
            gameState.messageLog.add(String.format("The %s is destroyed.", targetName));
        }
    }
}
