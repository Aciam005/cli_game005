package game.core.ecs.systems;

import game.core.ecs.Entity;
import game.core.ecs.components.AI;
import game.core.ecs.components.Flags;
import game.core.ecs.components.Position;
import game.core.ecs.components.Stats;
import game.core.game.GameState;
import game.util.Config;
import game.util.Rng;
import java.util.ArrayList;
import java.util.List;

public class CombatSystem {

    private final Rng rng;
    public final List<String> combatLogs = new ArrayList<>();

    public CombatSystem(Rng rng) {
        this.rng = rng;
    }

    public void applyDamage(GameState gameState, Entity target, int damage, String attackerName) {
        Stats oldStats = target.get(Stats.class).get();
        int newHp = oldStats.hp() - damage;

        Stats newStats = new Stats(newHp, oldStats.maxHp(), oldStats.atk(), oldStats.ev());
        target.add(newStats);

        String targetName = getEntityName(target);
        String message = String.format("%s hits %s for %d damage.", attackerName, targetName, damage);
        gameState.messageLog.add(message);
        combatLogs.add(message); // Also log to combat log for consistency

        if (newHp <= 0) {
            String deathMessage = String.format("The %s is destroyed.", targetName);
            gameState.messageLog.add(deathMessage);
            combatLogs.add(deathMessage);
            target.get(Flags.class).ifPresent(f -> f.isDead = true);
        }
    }

    public boolean handleAttack(GameState gameState, Entity attacker, Entity defender) {
        // If defender is player and in a vent, they are immune.
        if (defender.get(Flags.class).map(f -> f.isPlayer).orElse(false)) {
            if (defender.has(Position.class)) {
                Position pos = defender.get(Position.class).get();
                if (gameState.map.getTile(pos.x(), pos.y()).isVent()) {
                    return false; // Player is safe in vents
                }
            }
        }

        if (!attacker.has(Stats.class) || !defender.has(Stats.class)) {
            return false; // Cannot attack without stats
        }
        Stats attackerStats = attacker.get(Stats.class).get();
        Stats defenderStats = defender.get(Stats.class).get();

        // The entity's ATK is added to a dice roll, and this must beat a base defense value plus the defender's evasion (EV).
        int roll = rng.nextInt(Config.getInt("combat.dice_sides")) + 1;
        boolean hit = (roll + attackerStats.atk()) > (Config.getInt("combat.base_defense") + defenderStats.ev());

        String attackerName = getEntityName(attacker);
        String defenderName = getEntityName(defender);

        if (hit) {
            // Fixed damage of 1 for now.
            int damage = Config.getInt("combat.melee_damage");
            applyDamage(gameState, defender, damage, attackerName);
            return true;
        } else {
            String message = String.format("%s misses %s.", attackerName, defenderName);
            gameState.messageLog.add(message);
            combatLogs.add(message);
            return false;
        }
    }

    public String getEntityName(Entity entity) {
        if (entity.get(Flags.class).map(f -> f.isPlayer).orElse(false)) return "Player";
        if (entity.get(Flags.class).map(f -> f.isTurret).orElse(false)) return "Turret";
        if (entity.has(AI.class)) return "Drone";
        return "Entity";
    }
}
