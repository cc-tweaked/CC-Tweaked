/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

@FunctionalInterface
public interface ClientUnloadWorldEvent
{
    Event<ClientUnloadWorldEvent> EVENT = EventFactory.createArrayBacked( ClientUnloadWorldEvent.class,
        callbacks -> () -> {
            for( ClientUnloadWorldEvent callback : callbacks )
            {
                callback.onClientUnloadWorld();
            }
        } );

    void onClientUnloadWorld();
}
