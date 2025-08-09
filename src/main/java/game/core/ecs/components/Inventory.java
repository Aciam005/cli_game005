package game.core.ecs.components;

import game.core.ecs.Component;
import java.util.Map;
import java.util.HashMap;

public class Inventory implements Component {
    public final Map<String, Integer> items = new HashMap<>(); // e.g., "med-gel", "emp-charge"
}
