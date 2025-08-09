package game.core.ecs;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

public class Entity {
    private final List<Component> components = new ArrayList<>();

    public Entity(Component... components) {
        for (Component component : components) {
            this.add(component);
        }
    }

    public void add(Component component) {
        // Remove existing component of the same type before adding the new one.
        // This ensures an entity has at most one of each component type.
        components.removeIf(c -> c.getClass().equals(component.getClass()));
        components.add(component);
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> Optional<T> get(Class<T> componentClass) {
        for (Component c : components) {
            if (componentClass.isInstance(c)) {
                return Optional.of((T) c);
            }
        }
        return Optional.empty();
    }

    public <T extends Component> boolean has(Class<T> componentClass) {
        return get(componentClass).isPresent();
    }
}
