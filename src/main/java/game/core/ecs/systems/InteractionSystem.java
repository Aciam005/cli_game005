package game.core.ecs.systems;

import game.core.ecs.Entity;
import game.core.ecs.components.*;
import game.core.game.Direction;
import game.core.game.GameState;
import game.core.game.TurnEngine;
import game.core.map.Tile;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class InteractionSystem {

    public boolean handleInteraction(GameState gameState, TurnEngine turnEngine) {
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
            Entity targetEntity = turnEngine.getEntityAt(targetX, targetY);
            if (targetEntity != null) {
                if (targetEntity.has(Crate.class)) {
                    gameState.cratesCollected++;
                    gameState.entities.remove(targetEntity);
                    gameState.messageLog.add("You salvaged parts from the crate. (" + gameState.cratesCollected + "/3)");
                    return true;
                }
                if (targetEntity.has(Terminal.class)) {
                    Terminal terminal = targetEntity.get(Terminal.class).get();
                    if (!terminal.used()) {
                        gameState.messageLog.add("Terminal: " + terminal.loreText());
                        gameState.messageLog.add("The terminal pings the location of a nearby crate.");
                        targetEntity.add(new Terminal(terminal.loreText(), true));
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
}
