package game.core.game;

import game.core.ecs.Entity;
import game.core.map.TileMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;

public class GameState {
    public enum GameStatus {
        MENU,
        RUNNING,
        HELP,
        WIN,
        LOSE
    }

    public GameStatus status = GameStatus.MENU;
    public final TileMap map;
    public final List<Entity> entities = new ArrayList<>();
    public Entity player;
    public int cratesCollected = 0;
    public int turnsTaken = 0;
    public long seed;

    public boolean[][] visibleTiles;
    public final boolean[][] exploredTiles;
    public final Queue<NoiseEvent> noiseEvents = new LinkedList<>();
    public final List<String> messageLog = new ArrayList<>();

    public GameState(TileMap map) {
        this.map = map;
        this.visibleTiles = new boolean[map.getWidth()][map.getHeight()];
        this.exploredTiles = new boolean[map.getWidth()][map.getHeight()];
    }

    public void updateVisibility(boolean[][] newVisibility) {
        this.visibleTiles = newVisibility;
        for (int i = 0; i < map.getWidth(); i++) {
            for (int j = 0; j < map.getHeight(); j++) {
                if (visibleTiles[i][j]) {
                    exploredTiles[i][j] = true;
                }
            }
        }
    }
}
