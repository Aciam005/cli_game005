package game.core.ecs.systems;

import game.core.ecs.Entity;
import game.core.ecs.components.*;
import game.core.game.GameState;
import java.awt.Point;
import java.util.List;
import java.util.stream.Collectors;

public class ItemSystem {

    public boolean useMedGel(GameState gameState) {
        Entity player = gameState.player;
        Inventory inventory = player.get(Inventory.class).orElse(null);
        Stats stats = player.get(Stats.class).orElse(null);
        if (inventory == null || stats == null) return false;

        int medGelCount = inventory.items.getOrDefault("med-gel", 0);
        if (medGelCount > 0) {
            if (stats.hp() < stats.maxHp()) {
                int newHp = Math.min(stats.maxHp(), stats.hp() + 4);
                inventory.items.put("med-gel", medGelCount - 1);
                player.add(new Stats(newHp, stats.maxHp(), stats.atk(), stats.ev()));
                gameState.messageLog.add("You used a Med-gel and recovered some HP.");
                return true; // Action taken
            } else {
                gameState.messageLog.add("You are already at full health.");
                return false; // No action taken
            }
        } else {
            gameState.messageLog.add("You don't have any Med-gels.");
            return false;
        }
    }

    public boolean useEmpCharge(GameState gameState, Point target) {
        Entity player = gameState.player;
        Inventory inventory = player.get(Inventory.class).orElse(null);
        if (inventory == null) return false;

        int empChargeCount = inventory.items.getOrDefault("emp-charge", 0);
        if (empChargeCount > 0) {
            inventory.items.put("emp-charge", empChargeCount - 1);
            gameState.messageLog.add("You used an EMP charge.");

            List<Entity> affectedEntities = getEntitiesNear(gameState, target, 3);
            boolean isDisabled = false;
            for (Entity entity : affectedEntities) {
                if (entity.get(Flags.class).map(f -> f.isTurret || entity.has(AI.class)).orElse(false)) {
                    Flags flags = entity.get(Flags.class).get();
                    flags.isDisabled = true;
                    flags.disabledTurns = 5;
                    isDisabled = true;
                }
            }
            if(isDisabled) gameState.messageLog.add("An enemy system was disabled!");

            return true; // Action taken
        } else {
            gameState.messageLog.add("You don't have any EMP charges.");
            return false;
        }
    }

    private List<Entity> getEntitiesNear(GameState gameState, Point center, double radius) {
        return gameState.entities.stream()
            .filter(e -> e != gameState.player && e.has(Position.class) && new Point(e.get(Position.class).get().x(), e.get(Position.class).get().y()).distance(center) <= radius)
            .collect(Collectors.toList());
    }
}
