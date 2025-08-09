package game.core.map;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class Fov {

    public static boolean[][] computeFov(TileMap map, int playerX, int playerY, int radius) {
        boolean[][] visible = new boolean[map.getWidth()][map.getHeight()];

        visible[playerX][playerY] = true; // Player's tile is always visible

        // Iterate over a circle of tiles
        for (int i = playerX - radius; i <= playerX + radius; i++) {
            for (int j = playerY - radius; j <= playerY + radius; j++) {
                if (map.isInBounds(i, j)) {
                    if (Math.hypot(i - playerX, j - playerY) <= radius) {
                        revealLine(map, visible, playerX, playerY, i, j);
                    }
                }
            }
        }
        return visible;
    }

    private static void revealLine(TileMap map, boolean[][] visible, int x0, int y0, int x1, int y1) {
        for (Point p : game.util.Pathfinder.bresenhamLine(x0, y0, x1, y1)) {
            if (!map.isInBounds(p.x, p.y)) {
                break;
            }
            visible[p.x][p.y] = true;
            if (!map.getTile(p.x, p.y).isTransparent()) {
                break; // Stop at the first obstacle
            }
        }
    }
}
