package game.ui.cli;

import game.core.game.Game;

public class Main {

    public static void main(String[] args) {
        if (!"true".equals(System.getProperty("feature.signalRunner", "true"))) {
            System.out.println("Feature disabled");
            return;
        }

        long initialSeed = System.currentTimeMillis();
        for (String arg : args) {
            if (arg.startsWith("--seed")) {
                try {
                    initialSeed = Long.parseLong(arg.split("=")[1]);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid seed format. Using current time as seed.");
                }
            }
        }

        Game game = new Game(initialSeed);
        game.run();
    }
}
