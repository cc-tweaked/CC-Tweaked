package dan200.computercraft.fabric.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

@FunctionalInterface
public interface ClientUnloadWorldEvent
{
    Event<ClientUnloadWorldEvent> EVENT = EventFactory.createArrayBacked( ClientUnloadWorldEvent.class,
        callbacks -> () -> {
            for( ClientUnloadWorldEvent callback : callbacks) {
                callback.onClientUnloadWorld();
            }
    });

    void onClientUnloadWorld();
}
