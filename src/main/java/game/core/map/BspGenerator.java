package game.core.map;

import game.util.Rng;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BspGenerator {

    private static final int MIN_LEAF_SIZE = 8; // Min size of a partition
    private static final int MIN_ROOM_PADDING = 2; // Min padding from partition edge to room edge

    private final Rng rng;
    private final TileMap map;
    private final List<Rectangle> rooms = new ArrayList<>();
    private final Leaf root;

    private Point airlockLocation;
    private final List<Point> crateLocations = new ArrayList<>();
    private final List<Point> terminalLocations = new ArrayList<>();

    public BspGenerator(Rng rng, int mapWidth, int mapHeight) {
        this.rng = rng;
        this.map = new TileMap(mapWidth, mapHeight);
        this.root = new Leaf(new Rectangle(1, 1, mapWidth - 2, mapHeight - 2));
    }

    public TileMap generate() {
        // Split the map into leaves
        List<Leaf> leaves = new ArrayList<>();
        leaves.add(root);

        List<Leaf> finishedLeaves = new ArrayList<>();
        boolean didSplit = true;
        while (didSplit) {
            didSplit = false;
            List<Leaf> tempLeaves = new ArrayList<>();
            for (Leaf leaf : leaves) {
                if (leaf.split(rng)) {
                    tempLeaves.add(leaf.left);
                    tempLeaves.add(leaf.right);
                    didSplit = true;
                } else {
                    finishedLeaves.add(leaf);
                }
            }
            if (didSplit) {
                leaves = tempLeaves;
            }
        }
        finishedLeaves.addAll(leaves);

        // Create rooms in the leaves and carve them
        for (Leaf leaf : finishedLeaves) {
            leaf.createRoom(rng);
            if (leaf.room != null) {
                carveRoom(leaf.room);
            }
        }

        // Connect rooms
        connectRooms();

        // Place doors
        placeDoors();

        // Place features
        placeFeatures();

        return map;
    }

    public List<Rectangle> getRooms() {
        return rooms;
    }

    public Point getAirlockLocation() { return airlockLocation; }
    public List<Point> getCrateLocations() { return crateLocations; }
    public List<Point> getTerminalLocations() { return terminalLocations; }

    private void placeFeatures() {
        if (rooms.isEmpty()) return;

        List<Rectangle> shuffledRooms = new ArrayList<>(rooms);
        Collections.shuffle(shuffledRooms, new Random(rng.nextInt(Integer.MAX_VALUE)));

        // 1. Airlock
        if (!shuffledRooms.isEmpty()) {
            Rectangle airlockRoom = shuffledRooms.remove(0);
            airlockLocation = new Point(airlockRoom.x + airlockRoom.width / 2, airlockRoom.y + airlockRoom.height / 2);
            map.setTile(airlockLocation.x, airlockLocation.y, Tile.AIRLOCK);
        }

        // 2. Crates (3)
        for (int i = 0; i < 3 && !shuffledRooms.isEmpty(); i++) {
            Rectangle crateRoom = shuffledRooms.remove(0);
            Point cratePos = new Point(crateRoom.x + crateRoom.width / 2, crateRoom.y + crateRoom.height / 2);
            // Ensure we don't place over the airlock tile
            if (map.getTile(cratePos.x, cratePos.y) == Tile.FLOOR) {
                crateLocations.add(cratePos);
            }
        }

        // 3. Terminals (2)
        for (int i = 0; i < 2 && !shuffledRooms.isEmpty(); i++) {
            Rectangle terminalRoom = shuffledRooms.remove(0);
            Point terminalPos = new Point(terminalRoom.x + terminalRoom.width / 2, terminalRoom.y + terminalRoom.height / 2);
            if (map.getTile(terminalPos.x, terminalPos.y) == Tile.FLOOR) {
                terminalLocations.add(terminalPos);
            }
        }

        // 4. Bulkheads (2)
        placeBulkheads();
    }

    private void placeBulkheads() {
        List<Point> doorLocations = new ArrayList<>();
        for (int x = 1; x < map.getWidth() - 1; x++) {
            for (int y = 1; y < map.getHeight() - 1; y++) {
                if (map.getTile(x, y) == Tile.DOOR_CLOSED) {
                    doorLocations.add(new Point(x, y));
                }
            }
        }
        Collections.shuffle(doorLocations, new Random(rng.nextInt(Integer.MAX_VALUE)));
        for (int i = 0; i < 2 && !doorLocations.isEmpty(); i++) {
            Point bulkheadPos = doorLocations.remove(0);
            map.setTile(bulkheadPos.x, bulkheadPos.y, Tile.BULKHEAD_CLOSED);
        }
    }

    private void carveRoom(Rectangle room) {
        rooms.add(room);
        for (int x = room.x; x < room.x + room.width; x++) {
            for (int y = room.y; y < room.y + room.height; y++) {
                if (x == room.x || x == room.x + room.width -1 || y == room.y || y == room.y + room.height -1) {
                     // Keep walls for now, doors will be carved later
                } else {
                    map.setTile(x, y, Tile.FLOOR);
                }
            }
        }
    }

    private void connectRooms() {
        if (rooms.size() < 2) {
            return;
        }

        List<Rectangle> connected = new ArrayList<>();
        List<Rectangle> unconnected = new ArrayList<>(rooms);

        connected.add(unconnected.remove(0));

        while (!unconnected.isEmpty()) {
            Rectangle bestConnected = null;
            Rectangle bestUnconnected = null;
            double minDistance = Double.MAX_VALUE;

            for (Rectangle r1 : connected) {
                for (Rectangle r2 : unconnected) {
                    Point p1 = new Point(r1.x + r1.width / 2, r1.y + r1.height / 2);
                    Point p2 = new Point(r2.x + r2.width / 2, r2.y + r2.height / 2);
                    double distance = p1.distanceSq(p2);
                    if (distance < minDistance) {
                        minDistance = distance;
                        bestConnected = r1;
                        bestUnconnected = r2;
                    }
                }
            }

            if (bestConnected != null && bestUnconnected != null) {
                Point p1 = new Point(bestConnected.x + bestConnected.width / 2, bestConnected.y + bestConnected.height / 2);
                Point p2 = new Point(bestUnconnected.x + bestUnconnected.width / 2, bestUnconnected.y + bestUnconnected.height / 2);

                if (rng.nextInt(2) == 0) {
                    carveHCorridor(p1.x, p2.x, p1.y);
                    carveVCorridor(p1.y, p2.y, p2.x);
                } else {
                    carveVCorridor(p1.y, p2.y, p1.x);
                    carveHCorridor(p1.x, p2.x, p2.y);
                }

                unconnected.remove(bestUnconnected);
                connected.add(bestUnconnected);
            } else {
                break; // Should not happen
            }
        }
    }

    private void carveHCorridor(int x1, int x2, int y) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            map.setTile(x, y, Tile.FLOOR);
        }
    }

    private void carveVCorridor(int y1, int y2, int x) {
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
            map.setTile(x, y, Tile.FLOOR);
        }
    }

    private void placeDoors() {
        for (int x = 1; x < map.getWidth() - 1; x++) {
            for (int y = 1; y < map.getHeight() - 1; y++) {
                if (map.getTile(x, y) == Tile.WALL) {
                    boolean floorAbove = map.getTile(x, y - 1) == Tile.FLOOR;
                    boolean floorBelow = map.getTile(x, y + 1) == Tile.FLOOR;
                    boolean floorLeft = map.getTile(x - 1, y) == Tile.FLOOR;
                    boolean floorRight = map.getTile(x + 1, y) == Tile.FLOOR;

                    if ((floorAbove && floorBelow) && !(floorLeft || floorRight)) {
                        map.setTile(x, y, Tile.DOOR_CLOSED);
                    } else if ((floorLeft && floorRight) && !(floorAbove || floorBelow)) {
                        map.setTile(x, y, Tile.DOOR_CLOSED);
                    }
                }
            }
        }
    }

    private static class Leaf {
        private final Rectangle rect;
        public Leaf left = null;
        public Leaf right = null;
        public Rectangle room = null;

        public Leaf(Rectangle rect) {
            this.rect = rect;
        }

        public boolean split(Rng rng) {
            if (left != null || right != null) {
                return false;
            }

            boolean splitH = rng.nextDouble() > 0.5;
            if (rect.width > rect.height * 1.25) splitH = false;
            else if (rect.height > rect.width * 1.25) splitH = true;

            int max = (splitH ? rect.height : rect.width) - MIN_LEAF_SIZE;
            if (max <= MIN_LEAF_SIZE) {
                return false;
            }

            int split = rng.nextInt(max - MIN_LEAF_SIZE) + MIN_LEAF_SIZE;

            if (splitH) {
                left = new Leaf(new Rectangle(rect.x, rect.y, rect.width, split));
                right = new Leaf(new Rectangle(rect.x, rect.y + split, rect.width, rect.height - split));
            } else {
                left = new Leaf(new Rectangle(rect.x, rect.y, split, rect.height));
                right = new Leaf(new Rectangle(rect.x + split, rect.y, rect.width - split, rect.height));
            }
            return true;
        }

        public void createRoom(Rng rng) {
            if (left != null || right != null) {
                return;
            }

            int roomWidth = rng.nextInt(rect.width - MIN_ROOM_PADDING * 2) + MIN_ROOM_PADDING;
            int roomHeight = rng.nextInt(rect.height - MIN_ROOM_PADDING * 2) + MIN_ROOM_PADDING;
            int roomX = rect.x + rng.nextInt(rect.width - roomWidth -1);
            int roomY = rect.y + rng.nextInt(rect.height - roomHeight -1);
            room = new Rectangle(roomX, roomY, roomWidth, roomHeight);
        }

        public Rectangle getRoom(Rng rng) {
            if (room != null) {
                return room;
            }
            Rectangle lRoom = null;
            Rectangle rRoom = null;
            if (left != null) lRoom = left.getRoom(rng);
            if (right != null) rRoom = right.getRoom(rng);

            if (lRoom == null && rRoom == null) return null;
            if (lRoom == null) return rRoom;
            if (rRoom == null) return lRoom;

            if (rng.nextInt(2) == 0) {
                return lRoom;
            } else {
                return rRoom;
            }
        }
    }
}
