package game.core.ecs.components;

import game.core.ecs.Component;

public class Flags implements Component {
    public boolean isPlayer = false;
    public boolean isTurret = false;
    public boolean isDisabled = false;
    public int disabledTurns = 0;
    public boolean isDead = false;
}
