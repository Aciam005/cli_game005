package game.core.ecs.components;

import game.core.ecs.Component;

public record Stats(int hp, int maxHp, int atk, int ev) implements Component {
}
