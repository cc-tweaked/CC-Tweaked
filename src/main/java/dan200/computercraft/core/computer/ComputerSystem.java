/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.computer;

import dan200.computercraft.api.filesystem.IFileSystem;
import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.ComputerAccess;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.filesystem.FileSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

/**
 * Implementation of {@link IComputerAccess}/{@link IComputerSystem} for usage by externally registered APIs.
 *
 * @see dan200.computercraft.api.ComputerCraftAPI#registerAPIFactory(ILuaAPIFactory)
 * @see ILuaAPIFactory
 * @see ApiWrapper
 */
public class ComputerSystem extends ComputerAccess implements IComputerSystem
{
    private final IAPIEnvironment environment;

    ComputerSystem( IAPIEnvironment environment )
    {
        super( environment );
        this.environment = environment;
    }

    @Nonnull
    @Override
    public String getAttachmentName()
    {
        return "computer";
    }

    @Nullable
    @Override
    public IFileSystem getFileSystem()
    {
        FileSystem fs = environment.getFileSystem();
        return fs == null ? null : fs.getMountWrapper();
    }

    @Nullable
    @Override
    public String getLabel()
    {
        return environment.getLabel();
    }

    @Nonnull
    @Override
    public Map<String, IPeripheral> getAvailablePeripherals()
    {
        // TODO: Should this return peripherals on the current computer?
        return Collections.emptyMap();
    }

    @Nullable
    @Override
    public IPeripheral getAvailablePeripheral( @Nonnull String name )
    {
        return null;
    }
}
