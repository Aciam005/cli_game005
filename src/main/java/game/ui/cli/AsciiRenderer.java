package game.ui.cli;

import game.core.ecs.Entity;
import game.core.ecs.components.*;
import game.core.game.GameState;
import game.core.map.Tile;

import java.util.stream.Collectors;

public class AsciiRenderer {

    public void render(GameState gameState) {
        // A simple way to clear the console in terminals that support ANSI escape codes.
        System.out.print("\033[H\033[2J");
        System.out.flush();

        StringBuilder sb = new StringBuilder();
        drawMap(sb, gameState);
        drawHud(sb, gameState);
        System.out.println(sb.toString());
        // For now, let's also dump the message log for debugging
        gameState.messageLog.forEach(System.out::println);
        gameState.messageLog.clear();
    }

    private void drawMap(StringBuilder sb, GameState gameState) {
        char[][] grid = new char[gameState.map.getWidth()][gameState.map.getHeight()];

        for (int y = 0; y < gameState.map.getHeight(); y++) {
            for (int x = 0; x < gameState.map.getWidth(); x++) {
                if (gameState.visibleTiles[x][y]) {
                    grid[x][y] = getTileChar(gameState.map.getTile(x, y));
                } else if (gameState.exploredTiles[x][y]) {
                    // In a real GUI, this would be greyed out. Here, we just show the tile.
                    grid[x][y] = getTileChar(gameState.map.getTile(x, y));
                } else {
                    grid[x][y] = ' ';
                }
            }
        }

        for (Entity entity : gameState.entities) {
            if (entity.has(Position.class)) {
                Position pos = entity.get(Position.class).get();
                if (gameState.visibleTiles[pos.x()][pos.y()]) {
                    grid[pos.x()][pos.y()] = getEntityChar(entity);
                }
            }
        }

        for (int y = 0; y < gameState.map.getHeight(); y++) {
            for (int x = 0; x < gameState.map.getWidth(); x++) {
                sb.append(grid[x][y]);
            }
            sb.append('\n');
        }
    }

    private void drawHud(StringBuilder sb, GameState gameState) {
        sb.append("----------------------------------------\n");
        // HP
        gameState.player.get(Stats.class).ifPresent(stats ->
            sb.append(String.format("HP: %d/%d   ", stats.hp(), stats.maxHp()))
        );
        // Crates
        sb.append(String.format("Crates: %d/3   ", gameState.cratesCollected));
        // Inventory
        gameState.player.get(Inventory.class).ifPresent(inv -> {
            String items = inv.items.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> String.format("%s(%d)", e.getKey(), e.getValue()))
                .collect(Collectors.joining(" "));
            sb.append("Items: ").append(items);
        });
        sb.append("\n----------------------------------------\n");
    }

    private char getTileChar(Tile tile) {
        return switch (tile) {
            case WALL -> '#';
            case FLOOR -> '.';
            case DOOR_CLOSED -> '+';
            case DOOR_OPEN -> '-';
            case BULKHEAD_CLOSED -> '=';
            case BULKHEAD_OPEN -> '_';
            case AIRLOCK -> 'A';
        };
    }

    private char getEntityChar(Entity entity) {
        if (entity.get(Flags.class).map(f -> f.isPlayer).orElse(false)) return '@';
        if (entity.get(Flags.class).map(f -> f.isTurret).orElse(false)) return 'T';
        if (entity.has(AI.class)) return 'D';
        if (entity.has(Crate.class)) return 'C';
        if (entity.has(Terminal.class)) return '$';
        if (entity.has(Item.class)) return '*';
        return '?';
    }
}
