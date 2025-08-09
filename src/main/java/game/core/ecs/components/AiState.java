package game.core.ecs.components;

public enum AiState {
    PATROL,      // Moving along a set path or wandering
    INVESTIGATE, // Moving towards a noise source
    CHASE,       // Actively pursuing the player
    SEARCH       // Lost sight of player, moving to last known location
}
