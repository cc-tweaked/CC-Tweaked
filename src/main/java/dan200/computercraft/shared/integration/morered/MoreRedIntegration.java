/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration.morered;

import commoble.morered.api.ChanneledPowerSupplier;
import commoble.morered.api.MoreRedAPI;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.common.IBundledRedstoneBlock;
import dan200.computercraft.shared.util.CapabilityUtil;
import dan200.computercraft.shared.util.FixedPointTileEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MoreRedIntegration
{
    public static final String MOD_ID = "morered";

    private static final ResourceLocation ID = new ResourceLocation( ComputerCraft.MOD_ID, "morered" );

    private static final class BundledPowerSupplier implements ICapabilityProvider, ChanneledPowerSupplier
    {
        private final IBundledRedstoneBlock block;
        private final BlockEntity tile;
        private LazyOptional<ChanneledPowerSupplier> instance;

        private BundledPowerSupplier( IBundledRedstoneBlock block, BlockEntity tile )
        {
            this.block = block;
            this.tile = tile;
        }

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability( @Nonnull Capability<T> cap, @Nullable Direction side )
        {
            if( cap != MoreRedAPI.CHANNELED_POWER_CAPABILITY ) return LazyOptional.empty();

            if( tile.isRemoved() || !block.getBundledRedstoneConnectivity( tile.getLevel(), tile.getBlockPos(), side ) )
            {
                return LazyOptional.empty();
            }

            return (instance == null ? (instance = LazyOptional.of( () -> this )) : instance).cast();
        }

        @Override
        public int getPowerOnChannel( @Nonnull Level world, @Nonnull BlockPos wirePos, @Nonnull BlockState wireState, @Nullable Direction wireFace, int channel )
        {
            if( wireFace == null ) return 0;

            BlockPos pos = wirePos.relative( wireFace );
            BlockState state = world.getBlockState( pos );
            if( !(state.getBlock() instanceof IBundledRedstoneBlock) ) return 0;

            IBundledRedstoneBlock block = (IBundledRedstoneBlock) state.getBlock();
            return (block.getBundledRedstoneOutput( world, pos, wireFace.getOpposite() ) & (1 << channel)) != 0 ? 31 : 0;
        }

        void invalidate()
        {
            instance = CapabilityUtil.invalidate( instance );
        }
    }

    @SubscribeEvent
    public static void attachBlockCapabilities( AttachCapabilitiesEvent<BlockEntity> event )
    {
        BlockEntity tile = event.getObject();
        if( !(tile.getType() instanceof FixedPointTileEntityType) ) return;

        Block block = ((FixedPointTileEntityType<?>) tile.getType()).getBlock();
        if( !(block instanceof IBundledRedstoneBlock) ) return;

        BundledPowerSupplier provider = new BundledPowerSupplier( (IBundledRedstoneBlock) block, tile );
        event.addCapability( ID, provider );
        event.addListener( provider::invalidate );
    }

    public static void initialise()
    {
        MinecraftForge.EVENT_BUS.register( MoreRedIntegration.class );
        ComputerCraftAPI.registerBundledRedstoneProvider( MoreRedIntegration::getBundledPower );
    }

    private static int getBundledPower( Level world, BlockPos pos, Direction side )
    {
        BlockEntity tile = world.getBlockEntity( pos );
        if( tile == null ) return -1;

        ChanneledPowerSupplier power = CapabilityUtil.unwrapUnsafe( tile.getCapability( MoreRedAPI.CHANNELED_POWER_CAPABILITY, side ) );
        if( power == null ) return -1;

        BlockState state = tile.getBlockState();

        // Skip ones already handled by CC. We can do this more efficiently.
        if( state.getBlock() instanceof IBundledRedstoneBlock ) return -1;

        int mask = 0;
        for( int i = 0; i < 16; i++ )
        {
            mask |= power.getPowerOnChannel( world, pos, state, side, i ) > 0 ? (1 << i) : 0;
        }

        return mask;
    }
}
