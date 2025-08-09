package game;

import game.core.ecs.Entity;
import game.core.ecs.components.*;
import game.core.ecs.systems.AiDecisionSystem;
import game.core.game.GameState;
import game.core.map.TileMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.awt.Point;
import static org.junit.jupiter.api.Assertions.*;

public class AiDecisionSystemTest {

    private GameState gameState;
    private AiDecisionSystem decisionSystem;
    private Entity drone;

    @BeforeEach
    void setUp() {
        gameState = new GameState(new TileMap(10, 10));
        decisionSystem = new AiDecisionSystem();
        drone = new Entity(new Position(5, 5), new AI(), new Stats(1,1,1,1), new Flags());
        gameState.entities.add(drone);
        gameState.player = new Entity(new Position(1,1), new Flags(){{isPlayer=true;}});
        gameState.entities.add(gameState.player);
    }

    @Test
    void testPatrolToChase() {
        AiPerception perception = new AiPerception();
        perception.canSeePlayer = true;
        perception.lastKnownPlayerPosition = new Point(6, 6);
        drone.add(perception);

        decisionSystem.process(gameState);

        AI ai = drone.get(AI.class).get();
        assertEquals(AiState.CHASE, ai.state);
        assertEquals(new Point(6, 6), ai.targetPosition);
    }

    @Test
    void testPatrolToInvestigate() {
        AiPerception perception = new AiPerception();
        perception.noiseLocation = new Point(7, 7);
        drone.add(perception);

        decisionSystem.process(gameState);

        AI ai = drone.get(AI.class).get();
        assertEquals(AiState.INVESTIGATE, ai.state);
        assertEquals(new Point(7, 7), ai.targetPosition);
    }

    @Test
    void testChaseToSearch() {
        AI ai = drone.get(AI.class).get();
        ai.state = AiState.CHASE;

        AiPerception perception = new AiPerception();
        perception.canSeePlayer = false; // Lost sight
        drone.add(perception);

        decisionSystem.process(gameState);

        assertEquals(AiState.SEARCH, ai.state);
        assertEquals(5, ai.searchTurnsLeft);
    }
}
