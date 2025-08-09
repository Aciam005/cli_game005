package game.core.ecs.components;

import game.core.ecs.Component;

public class PlayerState implements Component {
    public enum WeaponMode {
        DEFAULT,
        PISTOL
    }

    public WeaponMode mode = WeaponMode.DEFAULT;
}
