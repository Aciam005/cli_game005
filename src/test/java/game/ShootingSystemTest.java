package game;

import game.core.ecs.Entity;
import game.core.ecs.components.*;
import game.core.game.Direction;
import game.core.game.GameState;
import game.core.ecs.systems.ShootingSystem;
import game.core.map.Tile;
import game.core.map.TileMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShootingSystemTest {

    private GameState gameState;
    private ShootingSystem shootingSystem;
    private Entity player;

    @BeforeEach
    void setUp() {
        TileMap map = new TileMap(10, 10);
        // Fill map with floor
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                map.setTile(x, y, Tile.FLOOR);
            }
        }
        gameState = new GameState(map);
        shootingSystem = new ShootingSystem();

        player = new Entity(
                new Position(1, 1),
                new Stats(10, 10, 1, 1),
                new Inventory(),
                new PlayerState(),
                new Flags() {{ isPlayer = true; }}
        );
        player.get(Inventory.class).get().items.put("ammo", 6);
        gameState.player = player;
        gameState.entities.add(player);
    }

    @Test
    void testFirePistol_hitsTarget() {
        Entity target = new Entity(new Position(1, 4), new Stats(4, 4, 1, 1), new Flags(), new AI());
        gameState.entities.add(target);

        boolean turnTaken = shootingSystem.fire(gameState, Direction.SOUTH);

        assertTrue(turnTaken);
        assertEquals(2, target.get(Stats.class).get().hp());
        assertEquals(5, player.get(Inventory.class).get().items.get("ammo"));
        assertEquals(1, gameState.noiseEvents.size());
        assertEquals(12, gameState.noiseEvents.peek().radius());
        assertTrue(gameState.messageLog.stream().anyMatch(s -> s.contains("hit the Drone for 2 damage")));
    }

    @Test
    void testFirePistol_blockedByWall() {
        gameState.map.setTile(1, 3, Tile.WALL);
        Entity target = new Entity(new Position(1, 4), new Stats(4, 4, 1, 1), new Flags(), new AI());
        gameState.entities.add(target);

        shootingSystem.fire(gameState, Direction.SOUTH);

        assertEquals(4, target.get(Stats.class).get().hp());
        assertTrue(gameState.messageLog.stream().anyMatch(s -> s.contains("hit a wall")));
    }

    @Test
    void testFirePistol_noAmmo() {
        player.get(Inventory.class).get().items.put("ammo", 0);
        Entity target = new Entity(new Position(1, 4), new Stats(4, 4, 1, 1), new Flags(), new AI());
        gameState.entities.add(target);

        boolean turnTaken = shootingSystem.fire(gameState, Direction.SOUTH);

        assertFalse(turnTaken);
        assertEquals(4, target.get(Stats.class).get().hp());
        assertTrue(gameState.messageLog.stream().anyMatch(s -> s.contains("Click. Out of ammo.")));
    }

    @Test
    void testFirePistol_hitsFirstTarget() {
        Entity target1 = new Entity(new Position(1, 3), new Stats(4, 4, 1, 1), new Flags(), new AI());
        Entity target2 = new Entity(new Position(1, 5), new Stats(4, 4, 1, 1), new Flags(), new AI());
        gameState.entities.add(target1);
        gameState.entities.add(target2);

        shootingSystem.fire(gameState, Direction.SOUTH);

        assertEquals(2, target1.get(Stats.class).get().hp());
        assertEquals(4, target2.get(Stats.class).get().hp());
    }
}
