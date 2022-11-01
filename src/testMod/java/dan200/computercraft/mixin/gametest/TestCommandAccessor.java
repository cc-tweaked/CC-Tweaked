/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.mixin.gametest;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.TestCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin( TestCommand.class )
public interface TestCommandAccessor
{
    @Invoker
    static int callExportTestStructure( CommandSourceStack source, String structure )
    {
        return 0;
    }
}
