package game.util;

import java.util.Random;

public class Rng {
    private final Random random;

    public Rng(long seed) {
        this.random = new Random(seed);
    }

    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    public double nextDouble() {
        return random.nextDouble();
    }
}
