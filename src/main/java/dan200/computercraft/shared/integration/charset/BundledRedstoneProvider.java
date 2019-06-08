/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration.charset;

import dan200.computercraft.api.redstone.IBundledRedstoneProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

import static dan200.computercraft.shared.integration.charset.IntegrationCharset.CAPABILITY_EMITTER;

public class BundledRedstoneProvider implements IBundledRedstoneProvider
{
    @Override
    public int getBundledRedstoneOutput( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Direction side )
    {
        TileEntity tile = world.getTileEntity( pos );
        if( tile == null || !tile.hasCapability( CAPABILITY_EMITTER, side ) ) return -1;

        byte[] signal = tile.getCapability( CAPABILITY_EMITTER, side ).getBundledSignal();
        if( signal == null ) return -1;

        int flag = 0;
        for( int i = 0; i < signal.length; i++ ) flag |= signal[i] > 0 ? 1 << i : 0;
        return flag;
    }
}
