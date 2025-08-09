package game.core.ecs.systems;

import game.core.ecs.Entity;
import game.core.ecs.components.Flags;
import game.core.ecs.components.Stats;
import game.util.Rng;
import java.util.ArrayList;
import java.util.List;

public class CombatSystem {

    private final Rng rng;
    public final List<String> combatLogs = new ArrayList<>();

    public CombatSystem(Rng rng) {
        this.rng = rng;
    }

    public boolean handleAttack(Entity attacker, Entity defender) {
        if (!attacker.has(Stats.class) || !defender.has(Stats.class)) {
            return false; // Cannot attack without stats
        }
        Stats attackerStats = attacker.get(Stats.class).get();
        Stats defenderStats = defender.get(Stats.class).get();

        // Simple ATK vs EV roll. Let's assume a d20 system.
        int roll = rng.nextInt(20) + 1;
        boolean hit = (roll + attackerStats.atk()) > (10 + defenderStats.ev());

        // A more descriptive name would be good, maybe from a 'Name' component later.
        String attackerName = attacker.get(Flags.class).map(f -> f.isPlayer ? "Player" : (f.isTurret ? "Turret" : "Drone")).orElse("Entity");
        String defenderName = defender.get(Flags.class).map(f -> f.isPlayer ? "Player" : (f.isTurret ? "Turret" : "Drone")).orElse("Entity");

        if (hit) {
            // Fixed damage of 1 for now.
            int damage = 1;
            int newHp = defenderStats.hp() - damage;

            Stats newDefenderStats = new Stats(newHp, defenderStats.maxHp(), defenderStats.atk(), defenderStats.ev());
            defender.add(newDefenderStats);

            combatLogs.add(String.format("%s hits %s for %d damage.", attackerName, defenderName, damage));

            if (newHp <= 0) {
                // A death system will handle removing the entity. For now, just log it.
                combatLogs.add(String.format("%s has been destroyed.", defenderName));
            }
            return true;
        } else {
            combatLogs.add(String.format("%s misses %s.", attackerName, defenderName));
            return false;
        }
    }
}
