/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.datafix;

import dan200.computercraft.ComputerCraft;
import net.minecraft.util.datafix.FixTypes;
import net.minecraftforge.common.util.CompoundDataFixer;
import net.minecraftforge.common.util.ModFixs;

public final class Fixes
{
    public static final int VERSION = 1;

    private Fixes() {}

    public static void register( CompoundDataFixer fixer )
    {
        ModFixs fixes = fixer.init( ComputerCraft.MOD_ID, VERSION );
        fixes.registerFix( FixTypes.BLOCK_ENTITY, new TileEntityDataFixer() );
    }
}
