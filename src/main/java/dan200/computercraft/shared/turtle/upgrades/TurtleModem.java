/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemPeripheral;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

public class TurtleModem extends AbstractTurtleUpgrade
{
    private final boolean advanced;
    @Environment( EnvType.CLIENT )
    private ModelResourceLocation leftOffModel;
    @Environment( EnvType.CLIENT )
    private ModelResourceLocation rightOffModel;
    @Environment( EnvType.CLIENT )
    private ModelResourceLocation leftOnModel;
    @Environment( EnvType.CLIENT )
    private ModelResourceLocation rightOnModel;

    public TurtleModem( boolean advanced, ResourceLocation id )
    {
        super( id,
            TurtleUpgradeType.PERIPHERAL,
            advanced ? ComputerCraftRegistry.ModBlocks.WIRELESS_MODEM_ADVANCED : ComputerCraftRegistry.ModBlocks.WIRELESS_MODEM_NORMAL );
        this.advanced = advanced;
    }

    @Override
    public IPeripheral createPeripheral( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return new Peripheral( turtle, advanced );
    }

    @Nonnull
    @Override
    public TurtleCommandResult useTool( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side, @Nonnull TurtleVerb verb, @Nonnull Direction dir )
    {
        return TurtleCommandResult.failure();
    }

    @Nonnull
    @Override
    @Environment( EnvType.CLIENT )
    public TransformedModel getModel( ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        loadModelLocations();

        boolean active = false;
        if( turtle != null )
        {
            CompoundTag turtleNBT = turtle.getUpgradeNBTData( side );
            active = turtleNBT.contains( "active" ) && turtleNBT.getBoolean( "active" );
        }

        return side == TurtleSide.LEFT ? TransformedModel.of( active ? leftOnModel : leftOffModel ) : TransformedModel.of( active ? rightOnModel : rightOffModel );
    }

    @Environment( EnvType.CLIENT )
    private void loadModelLocations()
    {
        if( leftOffModel == null )
        {
            if( advanced )
            {
                leftOffModel = new ModelResourceLocation( "computercraft:turtle_modem_advanced_off_left", "inventory" );
                rightOffModel = new ModelResourceLocation( "computercraft:turtle_modem_advanced_off_right", "inventory" );
                leftOnModel = new ModelResourceLocation( "computercraft:turtle_modem_advanced_on_left", "inventory" );
                rightOnModel = new ModelResourceLocation( "computercraft:turtle_modem_advanced_on_right", "inventory" );
            }
            else
            {
                leftOffModel = new ModelResourceLocation( "computercraft:turtle_modem_normal_off_left", "inventory" );
                rightOffModel = new ModelResourceLocation( "computercraft:turtle_modem_normal_off_right", "inventory" );
                leftOnModel = new ModelResourceLocation( "computercraft:turtle_modem_normal_on_left", "inventory" );
                rightOnModel = new ModelResourceLocation( "computercraft:turtle_modem_normal_on_right", "inventory" );
            }
        }
    }

    @Override
    public void update( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        // Advance the modem
        if( !turtle.getLevel().isClientSide )
        {
            IPeripheral peripheral = turtle.getPeripheral( side );
            if( peripheral instanceof Peripheral )
            {
                ModemState state = ((Peripheral) peripheral).getModemState();
                if( state.pollChanged() )
                {
                    turtle.getUpgradeNBTData( side )
                        .putBoolean( "active", state.isOpen() );
                    turtle.updateUpgradeNBTData( side );
                }
            }
        }
    }

    private static class Peripheral extends WirelessModemPeripheral
    {
        private final ITurtleAccess turtle;

        Peripheral( ITurtleAccess turtle, boolean advanced )
        {
            super( new ModemState(), advanced );
            this.turtle = turtle;
        }

        @Nonnull
        @Override
        public Level getLevel()
        {
            return turtle.getLevel();
        }

        @Nonnull
        @Override
        public Vec3 getPosition()
        {
            BlockPos turtlePos = turtle.getPosition();
            return new Vec3( turtlePos.getX(), turtlePos.getY(), turtlePos.getZ() );
        }

        @Override
        public boolean equals( IPeripheral other )
        {
            return this == other || (other instanceof Peripheral && ((Peripheral) other).turtle == turtle);
        }
    }
}
