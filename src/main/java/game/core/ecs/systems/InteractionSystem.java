package game.core.ecs.systems;

import game.core.ecs.Component;
import game.core.ecs.Entity;
import game.core.ecs.components.*;
import game.core.game.Direction;
import game.core.game.GameState;
import game.core.game.TurnEngine;
import game.core.map.Tile;
import game.util.Rng;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class InteractionSystem {

    public boolean handleInteraction(GameState gameState, TurnEngine turnEngine, Rng rng) {
        Position playerPos = gameState.player.get(Position.class).orElse(null);
        if (playerPos == null) return false;

        // 1. Pick up items on the same tile
        List<Entity> itemsToPickup = new ArrayList<>();
        for (Entity entity : gameState.entities) {
            if (entity != gameState.player && entity.has(Item.class)) {
                entity.get(Position.class).ifPresent(itemPos -> {
                    if (itemPos.x() == playerPos.x() && itemPos.y() == playerPos.y()) {
                        itemsToPickup.add(entity);
                    }
                });
            }
        }

        if (!itemsToPickup.isEmpty()) {
            for (Entity itemEntity : itemsToPickup) {
                Item item = itemEntity.get(Item.class).get();
                Inventory inventory = gameState.player.get(Inventory.class).get();
                inventory.items.put(item.name(), inventory.items.getOrDefault(item.name(), 0) + 1);
                gameState.entities.remove(itemEntity);
                gameState.messageLog.add("You picked up a " + item.name() + ".");
            }
            return true; // Pick up takes a turn
        }

        // 2. Interact with adjacent objects (doors, terminals, crates)
        for (Direction dir : Direction.values()) {
            if (dir == Direction.WAIT) continue;
            int targetX = playerPos.x() + dir.dx;
            int targetY = playerPos.y() + dir.dy;

            if (!gameState.map.isInBounds(targetX, targetY)) continue;

            // Interact with doors
            Tile targetTile = gameState.map.getTile(targetX, targetY);
            if (targetTile == Tile.DOOR_CLOSED) {
                gameState.map.setTile(targetX, targetY, Tile.DOOR_OPEN);
                gameState.messageLog.add("You open the door.");
                turnEngine.generateNoise(new Point(targetX, targetY), 6);
                return true;
            }
            if (targetTile == Tile.BULKHEAD_CLOSED) {
                gameState.messageLog.add("The bulkhead is sealed tight. It won't budge.");
                turnEngine.generateNoise(new Point(targetX, targetY), 3); // Failed action noise
                return true;
            }

            // Interact with entities
            Entity targetEntity = gameState.getEntityAt(targetX, targetY);
            if (targetEntity != null) {
                if (targetEntity.has(Crate.class)) {
                    gameState.cratesCollected++;
                    gameState.entities.remove(targetEntity);
                    gameState.messageLog.add("You salvaged parts from the crate. (" + gameState.cratesCollected + "/3)");
                    if (rng.nextDouble() < 0.10) { // 10% chance
                        Inventory inventory = gameState.player.get(Inventory.class).get();
                        inventory.items.put("ammo", inventory.items.getOrDefault("ammo", 0) + 2);
                        gameState.messageLog.add("You found a spare pistol magazine! [+2 Ammo]");
                    }
                    return true;
                }
                if (targetEntity.has(Terminal.class)) {
                    Terminal terminal = targetEntity.get(Terminal.class).get();
                    if (!terminal.used()) {
                        gameState.messageLog.add("Terminal: " + terminal.loreText());
                        if (rng.nextDouble() < 0.5) { // 50% chance for a hint
                            if (rng.nextInt(2) == 0) { // 50/50 for crate or vent
                                findNearestEntity(gameState, Crate.class)
                                    .ifPresentOrElse(
                                        p -> gameState.messageLog.add("The terminal pings the location of a nearby crate."),
                                        () -> gameState.messageLog.add("No crates detected nearby.")
                                    );
                            } else {
                                findNearestVentExit(gameState, new Point(playerPos.x(), playerPos.y()))
                                    .ifPresentOrElse(
                                        p -> gameState.messageLog.add("The terminal highlights a nearby maintenance vent access."),
                                        () -> gameState.messageLog.add("No vent access detected nearby.")
                                    );
                            }
                        } else {
                            gameState.messageLog.add("The terminal's diagnostic scan finds nothing of interest.");
                        }
                        targetEntity.add(new Terminal(terminal.loreText(), true)); // Mark as used
                    } else {
                        gameState.messageLog.add("The terminal screen is idle.");
                    }
                    return true;
                }
            }
        }

        gameState.messageLog.add("There is nothing to interact with here.");
        return false; // No action taken
    }

    private Optional<Point> findNearestEntity(GameState gameState, Class<? extends Component> componentClass) {
        Position playerPos = gameState.player.get(Position.class).get();
        Point playerPoint = new Point(playerPos.x(), playerPos.y());
        return gameState.entities.stream()
                .filter(e -> e.has(componentClass) && e.has(Position.class))
                .min(Comparator.comparingDouble(e -> {
                    Position entityPos = e.get(Position.class).get();
                    return playerPoint.distanceSq(entityPos.x(), entityPos.y());
                }))
                .flatMap(e -> e.get(Position.class))
                .map(p -> new Point(p.x(), p.y()));
    }

    private Optional<Point> findNearestVentExit(GameState gameState, Point playerPoint) {
        List<Point> ventExits = new ArrayList<>();
        for (int y = 0; y < gameState.map.getHeight(); y++) {
            for (int x = 0; x < gameState.map.getWidth(); x++) {
                if (gameState.map.getTile(x, y).isVent()) {
                    // An exit is a vent tile adjacent to a non-vent, walkable tile
                    for (Direction dir : Direction.values()) {
                        if (dir == Direction.WAIT) continue;
                        int nx = x + dir.dx;
                        int ny = y + dir.dy;
                        if (gameState.map.isInBounds(nx, ny)) {
                            Tile adjacentTile = gameState.map.getTile(nx, ny);
                            if (adjacentTile.isWalkable() && !adjacentTile.isVent()) {
                                ventExits.add(new Point(x, y));
                                break;
                            }
                        }
                    }
                }
            }
        }

        return ventExits.stream()
                .min(Comparator.comparingDouble(p -> p.distanceSq(playerPoint)));
    }
}
