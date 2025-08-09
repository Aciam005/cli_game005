package game.util;

import game.core.map.TileMap;
import java.awt.Point;
import java.util.*;

public class Pathfinder {

    public static List<Point> findPath(TileMap map, Point start, Point end) {
        Queue<Point> frontier = new LinkedList<>();
        frontier.add(start);

        Map<Point, Point> cameFrom = new HashMap<>();
        cameFrom.put(start, null);

        while (!frontier.isEmpty()) {
            Point current = frontier.poll();

            if (current.equals(end)) {
                return reconstructPath(cameFrom, current);
            }

            for (Point next : getNeighbors(map, current)) {
                if (!cameFrom.containsKey(next)) {
                    frontier.add(next);
                    cameFrom.put(next, current);
                }
            }
        }

        return Collections.emptyList(); // No path found
    }

    public static List<Point> findAiPath(TileMap map, Point start, Point end) {
        Queue<Point> frontier = new LinkedList<>();
        frontier.add(start);

        Map<Point, Point> cameFrom = new HashMap<>();
        cameFrom.put(start, null);

        while (!frontier.isEmpty()) {
            Point current = frontier.poll();

            if (current.equals(end)) {
                return reconstructPath(cameFrom, current);
            }

            for (Point next : getAiNeighbors(map, current)) {
                if (!cameFrom.containsKey(next)) {
                    frontier.add(next);
                    cameFrom.put(next, current);
                }
            }
        }

        return Collections.emptyList(); // No path found
    }

    public static List<Point> getNeighbors(TileMap map, Point p) {
        List<Point> neighbors = new ArrayList<>();
        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        for (int i = 0; i < 4; i++) {
            int newX = p.x + dx[i];
            int newY = p.y + dy[i];
            if (map.isInBounds(newX, newY) && map.getTile(newX, newY).isWalkable()) {
                neighbors.add(new Point(newX, newY));
            }
        }
        return neighbors;
    }

    public static List<Point> getAiNeighbors(TileMap map, Point p) {
        List<Point> neighbors = new ArrayList<>();
        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        for (int i = 0; i < 4; i++) {
            int newX = p.x + dx[i];
            int newY = p.y + dy[i];
            if (map.isInBounds(newX, newY)) {
                var tile = map.getTile(newX, newY);
                if (tile.isWalkable() && !tile.isVent()) {
                    neighbors.add(new Point(newX, newY));
                }
            }
        }
        return neighbors;
    }

    private static List<Point> reconstructPath(Map<Point, Point> cameFrom, Point current) {
        List<Point> path = new ArrayList<>();
        while (cameFrom.get(current) != null) {
            path.add(current);
            current = cameFrom.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    public static List<Point> bresenhamLine(int x0, int y0, int x1, int y1) {
        List<Point> line = new ArrayList<>();
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            line.add(new Point(x0, y0));
            if (x0 == x1 && y0 == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
        return line;
    }
}
