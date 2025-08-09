package game.core.ecs.components;

import game.core.ecs.Component;
import java.awt.Point;

public class AiPerception implements Component {
    public boolean canSeePlayer = false;
    public Point lastKnownPlayerPosition = null;
    public Point noiseLocation = null;
}
