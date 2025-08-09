package game.core.map;

import java.util.Arrays;

public class TileMap {
    private final int width;
    private final int height;
    private final Tile[][] tiles;

    public TileMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];
        for (Tile[] row : tiles) {
            Arrays.fill(row, Tile.WALL); // Default to walls
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Tile getTile(int x, int y) {
        if (isInBounds(x, y)) {
            return tiles[x][y];
        }
        return null; // Or throw exception
    }

    public void setTile(int x, int y, Tile tile) {
        if (isInBounds(x, y)) {
            tiles[x][y] = tile;
        }
    }

    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }
}
