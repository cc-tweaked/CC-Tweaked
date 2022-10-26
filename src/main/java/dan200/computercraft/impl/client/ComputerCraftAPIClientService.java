/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.impl.client;

import dan200.computercraft.api.client.ComputerCraftAPIClient;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.impl.Services;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Backing interface for {@link ComputerCraftAPIClient}
 * <p>
 * Do <strong>NOT</strong> directly reference this class. It exists for internal use by the API.
 */
@ApiStatus.Internal
public interface ComputerCraftAPIClientService
{
    static ComputerCraftAPIClientService get()
    {
        ComputerCraftAPIClientService instance = Instance.INSTANCE;
        return instance == null ? Services.raise( ComputerCraftAPIClientService.class, Instance.ERROR ) : instance;
    }

    <T extends ITurtleUpgrade> void registerTurtleUpgradeModeller( @Nonnull TurtleUpgradeSerialiser<T> serialiser, @Nonnull TurtleUpgradeModeller<T> modeller );

    final class Instance
    {
        static final @Nullable ComputerCraftAPIClientService INSTANCE;
        static final @Nullable Throwable ERROR;

        static
        {
            Services.LoadedService<ComputerCraftAPIClientService> helper = Services.tryLoad( ComputerCraftAPIClientService.class );
            INSTANCE = helper.instance();
            ERROR = helper.error();
        }

        private Instance()
        {
        }
    }
}
