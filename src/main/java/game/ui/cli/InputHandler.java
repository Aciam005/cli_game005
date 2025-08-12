package game.ui.cli;

import game.core.ecs.components.PlayerState;
import game.core.ecs.systems.ShootingSystem;
import game.core.game.Direction;
import game.core.game.Game;
import game.core.game.GameState;
import game.core.game.TurnEngine;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Scanner;

public class InputHandler {

    private Game game;
    private GameState gameState;
    private TurnEngine turnEngine;
    private ShootingSystem shootingSystem;
    private Scanner scanner;

    public InputHandler(Game game, GameState gameState, TurnEngine turnEngine, ShootingSystem shootingSystem, Scanner scanner) {
        this.game = game;
        this.gameState = gameState;
        this.turnEngine = turnEngine;
        this.shootingSystem = shootingSystem;
        this.scanner = scanner;
    }

    public boolean handleGameInput(char command) {
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
            case 'h' -> game.enterHelpState();
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
                                game.aimRay = new ArrayList<>(shootingSystem.rayPath);
                            }
                        }
                    }
                } else {
                    gameState.messageLog.add("Pistol not equipped.");
                }
                break;
            }
        }
        return turnTaken;
    }

    private String getInput() {
        try {
            return scanner.nextLine();
        } catch (java.util.NoSuchElementException e) {
            return "q"; // Default to quit on input stream close
        }
    }
}
