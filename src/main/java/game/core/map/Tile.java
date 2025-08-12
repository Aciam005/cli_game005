package game.core.map;

public enum Tile {
    WALL(false, false),
    FLOOR(true, true),
    DOOR_CLOSED(false, false),
    DOOR_OPEN(true, true),
    BULKHEAD_CLOSED(false, false), // One-way
    BULKHEAD_OPEN(true, true),     // One-way
    AIRLOCK(true, true),
    VENT(true, false);

    private final boolean walkable;
    private final boolean transparent;

    Tile(boolean walkable, boolean transparent) {
        this.walkable = walkable;
        this.transparent = transparent;
    }

    public boolean isWalkable() {
        return walkable;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public boolean isVent() {
        return this == VENT;
    }
}
