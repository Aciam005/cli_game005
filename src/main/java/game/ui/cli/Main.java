package game.ui.cli;

import game.core.ecs.Entity;
import game.core.ecs.components.*;
import game.core.game.Direction;
import game.core.game.GameState;
import game.core.game.TurnEngine;
import game.core.map.BspGenerator;
import game.core.map.Tile;
import game.util.Rng;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Main {
    private static GameState gameState;
    private static TurnEngine turnEngine;
    private static AsciiRenderer renderer;
    private static Scanner scanner;

    public static void main(String[] args) {
        if (!"true".equals(System.getProperty("feature.signalRunner", "true"))) {
            System.out.println("Feature disabled");
            return;
        }

        long seed = System.currentTimeMillis();
        for (String arg : args) {
            if (arg.startsWith("--seed")) {
                try {
                    seed = Long.parseLong(arg.split("=")[1]);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid seed format. Using current time as seed.");
                }
            }
        }

        initializeGame(seed);
        runGameLoop();
        scanner.close();
        System.out.println("Exiting game.");
    }

    private static void initializeGame(long seed) {
        Rng rng = new Rng(seed);
        BspGenerator generator = new BspGenerator(rng, 40, 20);
        gameState = new GameState(generator.generate());
        turnEngine = new TurnEngine(gameState, rng);
        renderer = new AsciiRenderer();
        scanner = new Scanner(System.in);

        // Player
        Point startPos = generator.getAirlockLocation();
        gameState.player = new Entity(
                new Position(startPos.x, startPos.y),
                new Stats(10, 10, 3, 2), // HP, MaxHP, ATK, EV
                new Inventory(),
                new Flags() {{ isPlayer = true; }}
        );
        gameState.player.get(Inventory.class).ifPresent(inv -> {
            inv.items.put("emp-charge", 1);
            inv.items.put("med-gel", 1);
        });
        gameState.entities.add(gameState.player);

        List<Rectangle> rooms = new ArrayList<>(generator.getRooms());
        rooms.removeIf(r -> r.contains(startPos)); // Don't spawn things in the start room
        Collections.shuffle(rooms, new Random(rng.nextInt(Integer.MAX_VALUE)));

        // Drones (3)
        for (int i = 0; i < 3 && !rooms.isEmpty(); i++) {
            Rectangle room = rooms.remove(0);
            gameState.entities.add(new Entity(
                new Position(room.x + room.width / 2, room.y + room.height / 2),
                new Stats(4, 4, 2, 1), new AI(), new Flags()
            ));
        }

        // Turrets (2)
        for (int i = 0; i < 2 && !rooms.isEmpty(); i++) {
            Rectangle room = rooms.remove(0);
            gameState.entities.add(new Entity(
                new Position(room.x + room.width / 2, room.y + room.height / 2),
                new Stats(3, 3, 4, 0), new Flags() {{ isTurret = true; }}
            ));
        }

        // Items (1 med-gel, 1 EMP)
        for (int i = 0; i < 1 && !rooms.isEmpty(); i++) {
            Rectangle room = rooms.remove(0);
            gameState.entities.add(new Entity(new Position(room.x + room.width / 2, room.y + room.height / 2), new Item("med-gel")));
        }
        for (int i = 0; i < 1 && !rooms.isEmpty(); i++) {
            Rectangle room = rooms.remove(0);
            gameState.entities.add(new Entity(new Position(room.x + room.width / 2, room.y + room.height / 2), new Item("emp-charge")));
        }

        // Crates are handled by the generator, but we need entities for them
        for (Point p : generator.getCrateLocations()) {
            gameState.entities.add(new Entity(new Position(p.x, p.y), new Crate()));
        }

        // Terminals
        for (Point p : generator.getTerminalLocations()) {
             gameState.entities.add(new Entity(new Position(p.x, p.y), new Terminal("Log 481: The drones are getting smarter...", false)));
        }

        turnEngine.updateFov(); // Initial FOV calculation
    }

    private static void runGameLoop() {
        boolean running = true;
        while (running) {
            renderer.render(gameState);

            System.out.print("Cmd (w,a,s,d,e,p,.,1,2,q): ");
            String input = scanner.nextLine().toLowerCase();
            if (input.isEmpty()) continue;
            char command = input.charAt(0);

            boolean turnTaken = false;
            switch (command) {
                case 'w' -> turnTaken = turnEngine.handleMove(Direction.NORTH);
                case 'a' -> turnTaken = turnEngine.handleMove(Direction.WEST);
                case 's' -> turnTaken = turnEngine.handleMove(Direction.SOUTH);
                case 'd' -> turnTaken = turnEngine.handleMove(Direction.EAST);
                case '.' -> turnTaken = true;
                case 'e' -> turnTaken = turnEngine.handleInteract();
                case '1' -> turnTaken = turnEngine.handleUseItem("med-gel", null);
                case '2' -> {
                    System.out.print("Target EMP at (x,y): ");
                    try {
                        String[] coords = scanner.nextLine().split(",");
                        int x = Integer.parseInt(coords[0].trim());
                        int y = Integer.parseInt(coords[1].trim());
                        turnTaken = turnEngine.handleUseItem("emp-charge", new Point(x, y));
                    } catch (Exception e) {
                        gameState.messageLog.add("Invalid target coordinates.");
                    }
                }
                case 'p' -> {
                    System.out.print("Peek direction (w,a,s,d): ");
                    String peekDirStr = scanner.nextLine().toLowerCase();
                    if (!peekDirStr.isEmpty()) {
                        Direction dir = switch (peekDirStr.charAt(0)) {
                            case 'w' -> Direction.NORTH; case 'a' -> Direction.WEST;
                            case 's' -> Direction.SOUTH; case 'd' -> Direction.EAST;
                            default -> null;
                        };
                        if (dir != null) {
                            turnEngine.handlePeek(dir);
                            turnTaken = true;
                        }
                    }
                }
                case 'q' -> running = false;
            }

            if (turnTaken) {
                turnEngine.processTurn();
            }

            if (gameState.player.get(Stats.class).get().hp() <= 0) {
                renderer.render(gameState);
                System.out.println("\n--- You have died. Game Over. ---");
                running = false;
            }
            Position playerPos = gameState.player.get(Position.class).get();
            if (gameState.map.getTile(playerPos.x(), playerPos.y()) == Tile.AIRLOCK && gameState.cratesCollected >= 3) {
                renderer.render(gameState);
                System.out.println("\n--- You collected all the crates and returned to the airlock. You win! ---");
                running = false;
            }
        }
    }
}
