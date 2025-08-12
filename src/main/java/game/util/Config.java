package game.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

    private static final Properties properties = new Properties();

    static {
        load();
    }

    private static void load() {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("game.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find game.properties");
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String getString(String key) {
        return properties.getProperty(key);
    }

    public static int getInt(String key) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException e) {
            System.err.println("Invalid integer value for key: " + key);
            return 0; // Return a default or throw an exception
        }
    }

    public static int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
