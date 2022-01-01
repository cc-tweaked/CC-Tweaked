/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.lua;

import dan200.computercraft.api.filesystem.IFileSystem;
import dan200.computercraft.api.peripheral.IComputerAccess;

import javax.annotation.Nullable;

/**
 * An interface passed to {@link ILuaAPIFactory} in order to provide additional information
 * about a computer.
 */
public interface IComputerSystem extends IComputerAccess
{
    /**
     * Get the file system for this computer.
     *
     * @return The computer's file system, or {@code null} if it is not initialised.
     */
    @Nullable
    IFileSystem getFileSystem();

    /**
     * Get the label for this computer.
     *
     * @return This computer's label, or {@code null} if it is not set.
     */
    @Nullable
    String getLabel();
}
