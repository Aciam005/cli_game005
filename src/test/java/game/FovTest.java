package game;

import game.core.ecs.Entity;
import game.core.ecs.components.Position;
import game.core.game.Direction;
import game.core.game.GameState;
import game.core.game.TurnEngine;
import game.core.map.Fov;
import game.core.map.Tile;
import game.core.map.TileMap;
import game.util.Rng;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FovTest {

    private TileMap map;

    @BeforeEach
    void setUp() {
        map = new TileMap(10, 10);
        // Create a simple room
        for (int x = 1; x < 9; x++) {
            for (int y = 1; y < 9; y++) {
                map.setTile(x, y, Tile.FLOOR);
            }
        }
    }

    @Test
    void testFovBlocking() {
        // Player at (2,2), target at (5,2)
        map.setTile(4, 2, Tile.WALL);
        boolean[][] fov = Fov.computeFov(map, 2, 2, 5);
        assertFalse(fov[5][2], "Wall should block FOV.");

        map.setTile(4, 2, Tile.FLOOR);
        fov = Fov.computeFov(map, 2, 2, 5);
        assertTrue(fov[5][2], "Empty floor should not block FOV.");
    }

    @Test
    void testPeekRevealsRay() {
        GameState gameState = new GameState(map);
        Rng rng = new Rng(1);
        TurnEngine turnEngine = new TurnEngine(gameState, rng);

        gameState.player = new Entity(new Position(2, 2));
        gameState.entities.add(gameState.player);

        turnEngine.updateFov(); // Compute initial FOV (radius 4)
        assertFalse(gameState.visibleTiles[8][2], "Tile outside of normal FOV should not be visible initially.");

        turnEngine.handlePeek(Direction.EAST);

        // Peek ray is 6 tiles long from the player, so it should reveal up to x=8
        assertTrue(gameState.visibleTiles[3][2], "Peek should reveal tile at x=3");
        assertTrue(gameState.visibleTiles[4][2], "Peek should reveal tile at x=4");
        assertTrue(gameState.visibleTiles[5][2], "Peek should reveal tile at x=5");
        assertTrue(gameState.visibleTiles[6][2], "Peek should reveal tile at x=6");
        assertTrue(gameState.visibleTiles[7][2], "Peek should reveal tile at x=7");
        assertTrue(gameState.visibleTiles[8][2], "Peek should reveal tile at x=8");
    }
}
