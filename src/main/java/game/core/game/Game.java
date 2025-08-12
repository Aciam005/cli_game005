package game.core.game;

import game.core.ecs.components.PlayerState;
import game.core.ecs.components.Position;
import game.core.ecs.components.Stats;
import game.core.ecs.systems.ShootingSystem;
import game.core.factories.GameFactory;
import game.ui.cli.AsciiRenderer;
import game.ui.cli.InputHandler;
import game.util.Config;
import game.util.Rng;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Game {

    private GameState gameState;
    private TurnEngine turnEngine;
    private AsciiRenderer renderer;
    private Scanner scanner;
    private ShootingSystem shootingSystem;
    private InputHandler inputHandler;
    public List<Point> aimRay = null;
    private GameState.GameStatus previousStatus;

    public Game(long initialSeed) {
        this.scanner = new Scanner(System.in);
        this.renderer = new AsciiRenderer();
        // Dummy GameState for the main menu
        this.gameState = new GameState(null);
        this.gameState.seed = initialSeed;
    }

    public void run() {
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
        scanner.close();
        System.out.println("Exiting game.");
    }

    private void startGame(long seed) {
        Rng rng = new Rng(seed);
        gameState = GameFactory.createGame(seed);
        turnEngine = new TurnEngine(gameState, rng);
        shootingSystem = new ShootingSystem(turnEngine.getCombatSystem());
        inputHandler = new InputHandler(this, gameState, turnEngine, shootingSystem, scanner);
        turnEngine.updateFov();
    }

    private void runGame() {
        renderer.render(gameState, aimRay);
        aimRay = null; // Clear ray after one frame

        System.out.print("Cmd (h for help): ");
        String input = getInput();
        if (input.isEmpty()) return;
        char command = input.toLowerCase().charAt(0);

        boolean turnTaken = inputHandler.handleGameInput(command);

        if (turnTaken) {
            turnEngine.processTurn();
        }

        // Check for win/loss conditions
        if (gameState.player.get(Stats.class).get().hp() <= 0) {
            gameState.status = GameState.GameStatus.LOSE;
        }
        Position playerPos = gameState.player.get(Position.class).get();
        if (gameState.map.getTile(playerPos.x(), playerPos.y()) == game.core.map.Tile.AIRLOCK && gameState.cratesCollected >= Config.getInt("game.win_condition.crates")) {
            gameState.status = GameState.GameStatus.WIN;
        }
    }

    private void handleMenu() {
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

    public void enterHelpState() {
        previousStatus = gameState.status;
        gameState.status = GameState.GameStatus.HELP;
    }

    private void handleHelp() {
        renderer.renderHelp();
        System.out.print("Press H to close: ");
        String input = getInput();
        if (input.isEmpty()) return;
        char command = input.toLowerCase().charAt(0);

        if (command == 'h') {
            gameState.status = previousStatus;
        }
    }

    private void handleEndScreen() {
        renderer.renderEndScreen(gameState);
        String input = getInput();
        if (input.isEmpty()) return;
        char command = input.toLowerCase().charAt(0);

        switch (command) {
            case 'r' -> startGame(gameState.seed); // Restart with the same seed
            case 'n' -> startGame(System.currentTimeMillis()); // Start new game
            case 'm' -> {
                gameState.status = GameState.GameStatus.MENU;
                // Reset game objects
                turnEngine = null;
                shootingSystem = null;
            }
            case 'q' -> gameState.status = null; // Signal to exit
        }
    }

    private String getInput() {
        try {
            return scanner.nextLine();
        } catch (java.util.NoSuchElementException e) {
            return "q"; // Default to quit on input stream close
        }
    }
}
