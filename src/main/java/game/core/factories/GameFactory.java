package game.core.factories;

import game.core.ecs.Entity;
import game.core.ecs.components.*;
import game.core.game.GameState;
import game.core.map.BspGenerator;
import game.core.map.Tile;
import game.util.Config;
import game.util.Rng;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameFactory {

    public static GameState createGame(long seed) {
        Rng rng = new Rng(seed);
        BspGenerator generator = new BspGenerator(rng, Config.getInt("map.width"), Config.getInt("map.height"));
        GameState gameState = new GameState(generator.generate());
        gameState.seed = seed;

        // Player
        Point startPos = generator.getAirlockLocation();
        Entity player = new Entity(
                new Position(startPos.x, startPos.y),
                new Stats(Config.getInt("player.hp"), Config.getInt("player.maxHp"), Config.getInt("player.atk"), Config.getInt("player.ev")),
                new Inventory(),
                new PlayerState(),
                new Flags() {{ isPlayer = true; }}
        );
        player.get(Inventory.class).ifPresent(inv -> {
            inv.items.put("emp-charge", Config.getInt("player.inventory.emp_charge"));
            inv.items.put("med-gel", Config.getInt("player.inventory.med_gel"));
            inv.items.put("ammo", Config.getInt("player.inventory.ammo"));
            inv.items.put("pistol", Config.getInt("player.inventory.pistol"));
        });
        player.get(PlayerState.class).ifPresent(ps -> ps.mode = PlayerState.WeaponMode.PISTOL);
        gameState.player = player;
        gameState.entities.add(player);

        List<Rectangle> rooms = new ArrayList<>(generator.getRooms());
        rooms.removeIf(r -> r.contains(startPos));
        Collections.shuffle(rooms, new Random(rng.nextInt(Integer.MAX_VALUE)));

        // Drones
        int droneCount = Config.getInt("enemies.drones.count");
        for (int i = 0; i < droneCount && !rooms.isEmpty(); i++) {
            Rectangle room = rooms.remove(0);
            gameState.entities.add(new Entity(
                new Position(room.x + room.width / 2, room.y + room.height / 2),
                new Stats(Config.getInt("enemies.drones.hp"), Config.getInt("enemies.drones.maxHp"), Config.getInt("enemies.drones.atk"), Config.getInt("enemies.drones.ev")), new AI(), new Flags()
            ));
        }

        // Turrets
        int turretCount = Config.getInt("enemies.turrets.count");
        for (int i = 0; i < turretCount && !rooms.isEmpty(); i++) {
            Rectangle room = rooms.remove(0);
            gameState.entities.add(new Entity(
                new Position(room.x + room.width / 2, room.y + room.height / 2),
                new Stats(Config.getInt("enemies.turrets.hp"), Config.getInt("enemies.turrets.maxHp"), Config.getInt("enemies.turrets.atk"), Config.getInt("enemies.turrets.ev")), new Flags() {{ isTurret = true; }}
            ));
        }

        // Items
        int medGelCount = Config.getInt("items.med_gel.count");
        for (int i = 0; i < medGelCount && !rooms.isEmpty(); i++) {
            Rectangle room = rooms.remove(0);
            gameState.entities.add(new Entity(new Position(room.x + room.width / 2, room.y + room.height / 2), new Item("med-gel")));
        }
        int empChargeCount = Config.getInt("items.emp_charge.count");
        for (int i = 0; i < empChargeCount && !rooms.isEmpty(); i++) {
            Rectangle room = rooms.remove(0);
            gameState.entities.add(new Entity(new Position(room.x + room.width / 2, room.y + room.height / 2), new Item("emp-charge")));
        }

        // Crates
        for (Point p : generator.getCrateLocations()) {
            gameState.entities.add(new Entity(new Position(p.x, p.y), new Crate()));
        }

        // Terminals
        for (Point p : generator.getTerminalLocations()) {
             gameState.entities.add(new Entity(new Position(p.x, p.y), new Terminal("Log 481: The drones are getting smarter...", false)));
        }

        gameState.status = GameState.GameStatus.RUNNING;
        return gameState;
    }
}
