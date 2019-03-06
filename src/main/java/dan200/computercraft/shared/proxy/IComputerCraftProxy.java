/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.proxy;

import dan200.computercraft.shared.command.text.TableBuilder;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.File;

public interface IComputerCraftProxy
{
    void preInit();

    void init();

    File getWorldDir( World world );

    default void playRecordClient( BlockPos pos, SoundEvent record, String info )
    {
    }

    default void showTableClient( TableBuilder table )
    {
    }
}
