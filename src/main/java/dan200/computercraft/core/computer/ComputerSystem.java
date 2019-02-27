/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import dan200.computercraft.api.filesystem.IFileSystem;
import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.core.apis.ComputerAccess;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.filesystem.FileSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Implementation of {@link IComputerAccess}/{@link IComputerSystem} for usage by externally registered APIs.
 *
 * @see dan200.computercraft.api.ComputerCraftAPI#registerAPIFactory(ILuaAPIFactory)
 * @see ILuaAPIFactory
 * @see ApiWrapper
 */
public class ComputerSystem extends ComputerAccess implements IComputerSystem
{
    private final IAPIEnvironment m_environment;

    ComputerSystem( IAPIEnvironment m_environment )
    {
        super( m_environment );
        this.m_environment = m_environment;
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
        FileSystem fs = m_environment.getFileSystem();
        return fs == null ? null : fs.getMountWrapper();
    }

    @Nullable
    @Override
    public String getLabel()
    {
        return m_environment.getLabel();
    }
}
