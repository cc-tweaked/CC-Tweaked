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
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class TurtleModem extends AbstractTurtleUpgrade
{
    private final boolean advanced;
    @Environment( EnvType.CLIENT )
    private ModelIdentifier leftOffModel;
    @Environment( EnvType.CLIENT )
    private ModelIdentifier rightOffModel;
    @Environment( EnvType.CLIENT )
    private ModelIdentifier leftOnModel;
    @Environment( EnvType.CLIENT )
    private ModelIdentifier rightOnModel;

    public TurtleModem( boolean advanced, Identifier id )
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
                leftOffModel = new ModelIdentifier( "computercraft:turtle_modem_advanced_off_left", "inventory" );
                rightOffModel = new ModelIdentifier( "computercraft:turtle_modem_advanced_off_right", "inventory" );
                leftOnModel = new ModelIdentifier( "computercraft:turtle_modem_advanced_on_left", "inventory" );
                rightOnModel = new ModelIdentifier( "computercraft:turtle_modem_advanced_on_right", "inventory" );
            }
            else
            {
                leftOffModel = new ModelIdentifier( "computercraft:turtle_modem_normal_off_left", "inventory" );
                rightOffModel = new ModelIdentifier( "computercraft:turtle_modem_normal_off_right", "inventory" );
                leftOnModel = new ModelIdentifier( "computercraft:turtle_modem_normal_on_left", "inventory" );
                rightOnModel = new ModelIdentifier( "computercraft:turtle_modem_normal_on_right", "inventory" );
            }
        }
    }

    @Override
    public void update( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        // Advance the modem
        if( !turtle.getWorld().isClient )
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
        public World getWorld()
        {
            return turtle.getWorld();
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            BlockPos turtlePos = turtle.getPosition();
            return new Vec3d( turtlePos.getX(), turtlePos.getY(), turtlePos.getZ() );
        }

        @Override
        public boolean equals( IPeripheral other )
        {
            return this == other || (other instanceof Peripheral && ((Peripheral) other).turtle == turtle);
        }
    }
}
