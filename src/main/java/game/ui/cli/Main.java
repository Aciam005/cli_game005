package game.ui.cli;

import game.core.ecs.Entity;
import game.core.ecs.components.*;
import game.core.ecs.systems.ShootingSystem;
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
    private static ShootingSystem shootingSystem;
    public static List<Point> aimRay = null;
    private static GameState.GameStatus previousStatus; // To handle returning from Help screen

    public static void main(String[] args) {
        if (!"true".equals(System.getProperty("feature.signalRunner", "true"))) {
            System.out.println("Feature disabled");
            return;
        }

        long initialSeed = System.currentTimeMillis();
        for (String arg : args) {
            if (arg.startsWith("--seed")) {
                try {
                    initialSeed = Long.parseLong(arg.split("=")[1]);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid seed format. Using current time as seed.");
                }
            }
        }

        scanner = new Scanner(System.in);
        renderer = new AsciiRenderer();

        // GameState is not initialized until the player starts a game
        gameState = new GameState(null); // Dummy GameState for MENU
        gameState.seed = initialSeed;

        run(); // Start the state machine

        scanner.close();
        System.out.println("Exiting game.");
    }

    private static void startGame(long seed) {
        Rng rng = new Rng(seed);
        BspGenerator generator = new BspGenerator(rng, 40, 20);
        gameState = new GameState(generator.generate());
        gameState.seed = seed;
        turnEngine = new TurnEngine(gameState, rng);
        shootingSystem = new ShootingSystem(turnEngine.getCombatSystem());

        // Player
        Point startPos = generator.getAirlockLocation();
        gameState.player = new Entity(
                new Position(startPos.x, startPos.y),
                new Stats(10, 10, 3, 2), // HP, MaxHP, ATK, EV
                new Inventory(),
                new PlayerState(),
                new Flags() {{ isPlayer = true; }}
        );
        gameState.player.get(Inventory.class).ifPresent(inv -> {
            inv.items.put("emp-charge", 1);
            inv.items.put("med-gel", 1);
            inv.items.put("ammo", 6);
            inv.items.put("pistol", 1);
        });
        gameState.player.get(PlayerState.class).ifPresent(ps -> ps.mode = PlayerState.WeaponMode.PISTOL);
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
        gameState.status = GameState.GameStatus.RUNNING;
    }

    private static void run() {
        boolean exitGame = false;
        while (!exitGame) {
            switch (gameState.status) {
                case MENU -> handleMenu();
                case RUNNING -> runGame();
                case HELP -> handleHelp();
                case WIN, LOSE -> handleEndScreen();
            }
            if (gameState.status == null) { // A null status is the signal to exit
                exitGame = true;
            }
        }
    }

    private static void runGame() {
        renderer.render(gameState, aimRay);
        aimRay = null; // Clear ray after one frame

        System.out.print("Cmd (h for help): ");
        String input = getInput();
        if (input.isEmpty()) return;
        char command = input.toLowerCase().charAt(0);

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
                String peekDirStr = getInput();
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
            case 'q' -> gameState.status = null; // Signal to exit
            case 'h' -> enterHelpState();
            case '3' -> {
                gameState.player.get(PlayerState.class).ifPresent(ps -> {
                    if (ps.mode == PlayerState.WeaponMode.DEFAULT) {
                        ps.mode = PlayerState.WeaponMode.PISTOL;
                        gameState.messageLog.add("Pistol equipped.");
                    } else {
                        ps.mode = PlayerState.WeaponMode.DEFAULT;
                        gameState.messageLog.add("Switched to default mode.");
                    }
                });
                break; // No turn taken
            }
            case 'f' -> {
                if (gameState.player.get(PlayerState.class).map(ps -> ps.mode == PlayerState.WeaponMode.PISTOL).orElse(false)) {
                    System.out.print("Fire direction (w,a,s,d): ");
                    String fireDirStr = getInput();
                    if (!fireDirStr.isEmpty()) {
                        Direction dir = switch (fireDirStr.charAt(0)) {
                            case 'w' -> Direction.NORTH; case 'a' -> Direction.WEST;
                            case 's' -> Direction.SOUTH; case 'd' -> Direction.EAST;
                            default -> null;
                        };
                        if (dir != null) {
                            turnTaken = shootingSystem.fire(gameState, dir);
                            if (turnTaken) {
                                aimRay = new ArrayList<>(shootingSystem.rayPath);
                            }
                        }
                    }
                } else {
                    gameState.messageLog.add("Pistol not equipped.");
                }
                break;
            }
        }

        if (turnTaken) {
            turnEngine.processTurn();
        }

        // Check for win/loss conditions
        if (gameState.player.get(Stats.class).get().hp() <= 0) {
            gameState.status = GameState.GameStatus.LOSE;
        }
        Position playerPos = gameState.player.get(Position.class).get();
        if (gameState.map.getTile(playerPos.x(), playerPos.y()) == Tile.AIRLOCK && gameState.cratesCollected >= 3) {
            gameState.status = GameState.GameStatus.WIN;
        }
    }

    private static void handleMenu() {
        renderer.renderMenu(gameState);
        String input = getInput();
        if (input.isEmpty()) return;
        char command = input.toLowerCase().charAt(0);

        switch (command) {
            case 's' -> startGame(System.currentTimeMillis());
            case 'h' -> enterHelpState();
            case 'q' -> gameState.status = null; // Signal to exit
        }
    }

    private static void enterHelpState() {
        previousStatus = gameState.status;
        gameState.status = GameState.GameStatus.HELP;
    }

    private static void handleHelp() {
        renderer.renderHelp();
        System.out.print("Press H to close: ");
        String input = getInput();
        if (input.isEmpty()) return;
        char command = input.toLowerCase().charAt(0);

        if (command == 'h') {
            gameState.status = previousStatus;
        }
    }

    private static void handleEndScreen() {
        renderer.renderEndScreen(gameState);
        String input = getInput();
        if (input.isEmpty()) return;
        char command = input.toLowerCase().charAt(0);

        switch (command) {
            case 'r' -> startGame(gameState.seed); // Restart with the same seed
            case 'n' -> startGame(System.currentTimeMillis()); // Start new game
            case 'm' -> gameState.status = GameState.GameStatus.MENU;
            case 'q' -> gameState.status = null; // Signal to exit
        }
    }

    private static String getInput() {
        try {
            return scanner.nextLine();
        } catch (java.util.NoSuchElementException e) {
            return "q"; // Default to quit on input stream close
        }
    }
}
