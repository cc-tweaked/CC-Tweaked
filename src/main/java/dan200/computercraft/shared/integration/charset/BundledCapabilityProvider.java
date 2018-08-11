/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.integration.charset;

import dan200.computercraft.shared.common.TileGeneric;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import pl.asie.charset.api.wires.IBundledEmitter;
import pl.asie.charset.api.wires.IBundledReceiver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static dan200.computercraft.shared.integration.charset.IntegrationCharset.CAPABILITY_EMITTER;
import static dan200.computercraft.shared.integration.charset.IntegrationCharset.CAPABILITY_RECEIVER;

final class BundledCapabilityProvider implements ICapabilityProvider
{
    private final TileGeneric tile;
    private IBundledReceiver receiver;
    private IBundledEmitter[] emitters;

    BundledCapabilityProvider( TileGeneric tile )
    {
        this.tile = tile;
    }

    @Override
    public boolean hasCapability( @Nonnull Capability<?> capability, @Nullable EnumFacing side )
    {
        return capability == CAPABILITY_EMITTER || capability == CAPABILITY_RECEIVER;
    }

    @Nullable
    @Override
    public <T> T getCapability( @Nonnull Capability<T> capability, @Nullable EnumFacing side )
    {
        if( capability == CAPABILITY_RECEIVER )
        {
            IBundledReceiver receiver = this.receiver;
            if( receiver == null ) receiver = this.receiver = tile::onNeighbourChange;

            return CAPABILITY_RECEIVER.cast( receiver );
        }
        else if( capability == CAPABILITY_EMITTER )
        {
            IBundledEmitter[] emitters = this.emitters;
            if( emitters == null ) emitters = this.emitters = new IBundledEmitter[7];

            int index = side == null ? 6 : side.getIndex();
            IBundledEmitter emitter = emitters[index];
            if( emitter == null )
            {
                if( side == null )
                {
                    emitter = emitters[index] = () -> {
                        int flags = 0;
                        for( EnumFacing facing : EnumFacing.VALUES ) flags |= tile.getBundledRedstoneOutput( facing );
                        return toBytes( flags );
                    };
                }
                else
                {
                    emitter = emitters[index] = () -> toBytes( tile.getBundledRedstoneOutput( side ) );
                }
            }
            ;

            return CAPABILITY_EMITTER.cast( emitter );
        }
        else
        {
            return null;
        }
    }

    private static byte[] toBytes( int flag )
    {
        byte[] channels = new byte[16];
        for( int i = 0; i < 16; i++ ) channels[i] = (flag & (1 << i)) == 0 ? (byte) 0 : 15;
        return channels;
    }
}
