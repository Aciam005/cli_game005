package game.core.ecs.components;

import game.core.ecs.Component;

import java.awt.Point;
import java.util.List;

public class AI implements Component {
    public AiState state = AiState.PATROL;
    public Point targetPosition; // For investigating or chasing
    public int searchTurnsLeft = 0;
    public int campTurnsLeft = 0;
    public List<Point> currentPath;

    public AI() {
        this.targetPosition = null;
        this.currentPath = null;
    }
}
