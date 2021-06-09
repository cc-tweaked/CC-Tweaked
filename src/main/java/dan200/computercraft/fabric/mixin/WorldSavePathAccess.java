/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import net.minecraft.util.WorldSavePath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin( WorldSavePath.class )
public interface WorldSavePathAccess
{
    @Invoker( "<init>" )
    static WorldSavePath createWorldSavePath( String relativePath ) { throw new UnsupportedOperationException(); }
}
