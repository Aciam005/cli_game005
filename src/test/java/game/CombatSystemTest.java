package game;

import game.core.ecs.Entity;
import game.core.ecs.components.Flags;
import game.core.ecs.components.Position;
import game.core.ecs.components.Stats;
import game.core.ecs.systems.CombatSystem;
import game.core.ecs.systems.TurretSystem;
import game.core.game.GameState;
import game.core.map.Tile;
import game.core.map.TileMap;
import game.util.Rng;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CombatSystemTest {

    private GameState gameState;
    private CombatSystem combatSystem;
    private TurretSystem turretSystem;
    private Entity player;
    private Entity turret;

    @BeforeEach
    void setUp() {
        TileMap map = new TileMap(10, 10);
        for(int i=0; i<10; i++) for(int j=0; j<10; j++) map.setTile(i, j, Tile.FLOOR);
        gameState = new GameState(map);
        Rng rng = new Rng(1); // Use a fixed seed for predictable combat rolls
        combatSystem = new CombatSystem(rng);
        turretSystem = new TurretSystem(combatSystem);

        player = new Entity(new Position(2, 2), new Stats(10, 10, 5, 5), new Flags(){{isPlayer=true;}});
        turret = new Entity(new Position(8, 2), new Stats(5, 5, 5, 0), new Flags(){{isTurret=true;}});

        gameState.player = player;
        gameState.entities.add(player);
        gameState.entities.add(turret);
    }

    @Test
    void testTurretSeesPlayerAndAttacks() {
        // Give turret a high ATK for a guaranteed hit, overriding the BeforeEach setup for this test
        turret.add(new Stats(5, 5, 100, 0));
        turretSystem.process(gameState);
        assertEquals(9, player.get(Stats.class).get().hp(), "Player should lose HP when turret has clear LoS.");
    }

    @Test
    void testTurretLoSBlocked() {
        gameState.map.setTile(5, 2, Tile.WALL);
        turretSystem.process(gameState);
        assertEquals(10, player.get(Stats.class).get().hp(), "Player should not lose HP when turret LoS is blocked.");
    }

    @Test
    void testCombatGuaranteedHit() {
        // Attacker with 100 ATK, Defender with 0 EV. Should always hit.
        Entity attacker = new Entity(new Stats(1, 1, 100, 0), new Flags());
        Entity defender = new Entity(new Stats(10, 10, 0, 0), new Flags());

        boolean hit = combatSystem.handleAttack(gameState, attacker, defender);

        assertTrue(hit, "Attack should be a guaranteed hit.");
        assertEquals(9, defender.get(Stats.class).get().hp(), "Defender should lose 1 HP.");
    }

    @Test
    void testCombatGuaranteedMiss() {
        // Attacker with 0 ATK, Defender with 100 EV. Should always miss.
        Entity attacker = new Entity(new Stats(1, 1, 0, 0), new Flags());
        Entity defender = new Entity(new Stats(10, 10, 0, 100), new Flags());

        boolean hit = combatSystem.handleAttack(gameState, attacker, defender);

        assertFalse(hit, "Attack should be a guaranteed miss.");
        assertEquals(10, defender.get(Stats.class).get().hp(), "Defender HP should be unchanged.");
    }
}
