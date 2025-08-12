package game.ui.cli;

import game.core.ecs.Entity;
import game.core.ecs.components.*;
import game.core.ecs.components.*;
import game.core.game.GameState;
import game.core.map.Tile;

import java.awt.Point;
import java.util.List;
import java.util.stream.Collectors;

public class AsciiRenderer {

    public void render(GameState gameState, List<Point> aimRay) {
        // A simple way to clear the console in terminals that support ANSI escape codes.
        System.out.print("\033[H\033[2J");
        System.out.flush();

        StringBuilder sb = new StringBuilder();
        drawMap(sb, gameState, aimRay);
        drawHud(sb, gameState);
        System.out.println(sb.toString());
        // For now, let's also dump the message log for debugging
        gameState.messageLog.forEach(System.out::println);
        gameState.messageLog.clear();
    }

    private void drawMap(StringBuilder sb, GameState gameState, List<Point> aimRay) {
        char[][] grid = new char[gameState.map.getWidth()][gameState.map.getHeight()];

        for (int y = 0; y < gameState.map.getHeight(); y++) {
            for (int x = 0; x < gameState.map.getWidth(); x++) {
                if (gameState.visibleTiles[x][y]) {
                    grid[x][y] = getTileChar(gameState.map.getTile(x, y));
                } else if (gameState.exploredTiles[x][y]) {
                    // Make explored tiles dimmer. For now, just change floor.
                    if (gameState.map.getTile(x, y) == Tile.FLOOR) {
                        grid[x][y] = ',';
                    } else {
                        grid[x][y] = getTileChar(gameState.map.getTile(x, y));
                    }
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

        if (aimRay != null) {
            for (Point p : aimRay) {
                if (gameState.map.isInBounds(p.x, p.y) && gameState.visibleTiles[p.x][p.y]) {
                    grid[p.x][p.y] = '+';
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
        // Right-aligned info
        String rightAligned = "";
        if (gameState.player != null) {
            rightAligned = String.format("Coord: (%d,%d) Seed: %d Mode: %s Ammo: %d Crates: %d/3",
                gameState.player.get(Position.class).map(Position::x).orElse(0),
                gameState.player.get(Position.class).map(Position::y).orElse(0),
                gameState.seed,
                gameState.player.get(PlayerState.class).map(ps -> ps.mode.name()).orElse("N/A"),
                gameState.player.get(Inventory.class).map(inv -> inv.items.getOrDefault("ammo", 0)).orElse(0),
                gameState.cratesCollected);
        }

        // Left-aligned info
        String leftAligned = "";
        if (gameState.player != null) {
            leftAligned = String.format("HP: %d/%d",
                gameState.player.get(Stats.class).map(Stats::hp).orElse(0),
                gameState.player.get(Stats.class).map(Stats::maxHp).orElse(0));
        }

        int totalWidth = 40; // Default width if map is not available
        if (gameState.map != null) {
            totalWidth = gameState.map.getWidth();
        }
        int spacing = totalWidth - leftAligned.length() - rightAligned.length();
        String hudLine = leftAligned + " ".repeat(Math.max(1, spacing)) + rightAligned;

        sb.append("----------------------------------------\n");
        sb.append(hudLine).append("\n");

        if (gameState.player != null && gameState.player.has(Inventory.class)) {
            String items = gameState.player.get(Inventory.class).get().items.entrySet().stream()
                .filter(e -> e.getValue() > 0 && !e.getKey().equals("ammo"))
                .map(e -> String.format("%s(%d)", e.getKey(), e.getValue()))
                .collect(Collectors.joining(" "));
            sb.append("Items: ").append(items).append("\n");
        }

        sb.append("----------------------------------------\n");
    }

    public void renderMenu(GameState gameState) {
        clearScreen();
        System.out.println("Signal Runner");
        System.out.println("v0.1.0-alpha");
        System.out.println("\nOptions:");
        System.out.println("  S - Start (random seed)");
        System.out.println("  Q - Quit");
        System.out.println("\nPress H in-game for controls.");
        System.out.print("\n> ");
    }

    public void renderHelp() {
        clearScreen();
        System.out.println("--- Controls & Legend ---\n");
        System.out.println("Controls:");
        System.out.println("  WASD  - Move");
        System.out.println("  .     - Wait");
        System.out.println("  P+dir - Peek");
        System.out.println("  E     - Interact (Doors, Terminals, Crates)");
        System.out.println("  1     - Use Med-gel");
        System.out.println("  2     - Use EMP Charge (requires target)");
        System.out.println("  3     - Toggle Pistol");
        System.out.println("  F+dir - Fire Pistol");
        System.out.println("  Q     - Quit Game\n");
        System.out.println("Legend:");
        System.out.println("  @ - Player     d - Drone        T - Turret");
        System.out.println("  v - Vent       B - Bulkhead     + - Shot Ray");
        System.out.println("  . - Floor      # - Wall         D - Door");
        System.out.println("  A - Airlock    $ - Crate        Ƭ - Terminal");
    }

    public void renderEndScreen(GameState gameState) {
        clearScreen();
        if (gameState.status == GameState.GameStatus.WIN) {
            System.out.println("--- You Win! ---");
            System.out.println("You escaped the facility.");
        } else {
            System.out.println("--- You Lose! ---");
            System.out.println("You have perished in the depths of the facility.");
        }

        System.out.println("\n--- Game Summary ---");
        System.out.printf("Crates collected: %d\n", gameState.cratesCollected);
        System.out.printf("Turns taken: %d\n", gameState.turnsTaken);
        System.out.printf("Seed used: %d\n", gameState.seed);

        System.out.println("\nOptions:");
        System.out.println("  R - Restart (same seed)");
        System.out.println("  N - New Game (new seed)");
        System.out.println("  M - Main Menu");
        System.out.println("  Q - Quit");
        System.out.print("\n> ");
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
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
            case VENT -> 'v';
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
