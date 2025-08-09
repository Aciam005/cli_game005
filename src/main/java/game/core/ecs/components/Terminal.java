package game.core.ecs.components;

import game.core.ecs.Component;

public record Terminal(String loreText, boolean used) implements Component {
}
