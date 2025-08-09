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
    void testMapConnectivityWithoutVents() {
        Rng rng = new Rng(System.currentTimeMillis());
        BspGenerator generator = new BspGenerator(rng, 40, 20);
        TileMap map = generator.generate();

        Point airlock = generator.getAirlockLocation();
        List<Point> crates = generator.getCrateLocations();

        assertNotNull(airlock, "Airlock should be placed.");
        assertTrue(!crates.isEmpty() || generator.getRooms().size() < 4, "Crates should be placed if there are enough rooms.");

        for (Point crate : crates) {
            // Use findAiPath to ensure vents are not used for the main path
            List<Point> path = Pathfinder.findAiPath(map, airlock, crate);
            assertFalse(path.isEmpty(), "Should be a path from airlock to crate at " + crate + " without using vents.");
        }
    }
}
