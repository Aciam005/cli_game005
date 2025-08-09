package game;

import game.core.map.BspGenerator;
import game.core.map.TileMap;
import game.util.Pathfinder;
import game.util.Rng;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.List;

public class MapGeneratorTest {

    @RepeatedTest(10) // Run a few times with different seeds to be sure
    void testMapConnectivity() {
        Rng rng = new Rng(System.currentTimeMillis());
        BspGenerator generator = new BspGenerator(rng, 40, 20);
        TileMap map = generator.generate();

        Point airlock = generator.getAirlockLocation();
        List<Point> crates = generator.getCrateLocations();

        assertNotNull(airlock, "Airlock should be placed.");
        assertTrue(!crates.isEmpty() || generator.getRooms().size() < 4, "Crates should be placed if there are enough rooms.");

        for (Point crate : crates) {
            Point startPoint = findWalkableNeighbor(map, airlock);
            assertNotNull(startPoint, "Should be a walkable tile next to the airlock.");

            List<Point> path = Pathfinder.findPath(map, startPoint, crate);
            assertFalse(path.isEmpty(), "Should be a path from airlock to crate at " + crate);
        }
    }

    private Point findWalkableNeighbor(TileMap map, Point p) {
        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};
        for (int i = 0; i < 4; i++) {
            int newX = p.x + dx[i];
            int newY = p.y + dy[i];
            if (map.isInBounds(newX, newY) && map.getTile(newX, newY).isWalkable()) {
                return new Point(newX, newY);
            }
        }
        return null;
    }
}
