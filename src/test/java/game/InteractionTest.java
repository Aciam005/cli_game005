package game;

import game.core.ecs.Entity;
import game.core.ecs.components.Flags;
import game.core.ecs.components.Inventory;
import game.core.ecs.components.Position;
import game.core.ecs.components.Stats;
import game.core.ecs.systems.InteractionSystem;
import game.core.ecs.systems.ItemSystem;
import game.core.ecs.systems.TurretSystem;
import game.core.game.GameState;
import game.core.game.TurnEngine;
import game.core.map.Tile;
import game.core.map.TileMap;
import game.util.Rng;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.awt.Point;
import static org.junit.jupiter.api.Assertions.*;

public class InteractionTest {

    private GameState gameState;
    private TurnEngine turnEngine;
    private InteractionSystem interactionSystem;
    private ItemSystem itemSystem;
    private TurretSystem turretSystem;

    @BeforeEach
    void setUp() {
        TileMap map = new TileMap(10, 10);
        for(int i=0; i<10; i++) for(int j=0; j<10; j++) map.setTile(i, j, Tile.FLOOR);
        gameState = new GameState(map);
        Rng rng = new Rng(1);
        turnEngine = new TurnEngine(gameState, rng);
        interactionSystem = new InteractionSystem();
        itemSystem = new ItemSystem();
        // This is a bit awkward, ideally we'd have a way to get systems from the engine
        // For the test, we can create a new combat system instance for the turret system
        turretSystem = new TurretSystem(new game.core.ecs.systems.CombatSystem(rng));

        gameState.player = new Entity(new Position(5, 5), new Inventory(), new Stats(10,10,1,1), new Flags(){{isPlayer=true;}});
        gameState.entities.add(gameState.player);
    }

    @Test
    void testBulkheadInteraction() {
        gameState.map.setTile(6, 5, Tile.BULKHEAD_CLOSED);

        boolean turnTaken = interactionSystem.handleInteraction(gameState, turnEngine);

        assertTrue(turnTaken, "Interacting with a bulkhead should take a turn.");
        assertEquals(Tile.BULKHEAD_CLOSED, gameState.map.getTile(6, 5), "Bulkhead should remain closed.");
        assertEquals(1, gameState.noiseEvents.size(), "A noise event should be generated.");
        assertTrue(gameState.messageLog.stream().anyMatch(s -> s.contains("sealed tight")), "A message should be logged.");
    }

    @Test
    void testEmpDisablesTurret() {
        Entity turret = new Entity(new Position(5, 6), new Flags() {{ isTurret = true; }}, new Stats(3,3,1,1));
        gameState.entities.add(turret);
        gameState.player.get(Inventory.class).get().items.put("emp-charge", 1);

        itemSystem.useEmpCharge(gameState, new Point(5, 6));

        Flags turretFlags = turret.get(Flags.class).get();
        assertTrue(turretFlags.isDisabled, "Turret should be disabled after EMP.");
        assertEquals(5, turretFlags.disabledTurns, "Disabled turns should be set to 5.");

        // Simulate 4 turns passing
        for(int i=0; i<4; i++) {
            turretSystem.process(gameState);
            assertTrue(turretFlags.isDisabled, "Turret should remain disabled.");
        }

        // 5th turn
        turretSystem.process(gameState);
        assertFalse(turretFlags.isDisabled, "Turret should be re-enabled after 5 turns.");
    }
}
