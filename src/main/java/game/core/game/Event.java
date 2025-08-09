package game.core.game;

public enum Event {
    // Noise events
    NOISE_GENERATED,

    // Combat events
    ATTACK,
    DAMAGE,
    DEATH,

    // Item events
    ITEM_USED,

    // Game state events
    CRATE_COLLECTED,
    PLAYER_WINS,
    PLAYER_LOSES
}
