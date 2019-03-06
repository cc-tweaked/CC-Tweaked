/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemPeripheral;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;

public class TurtleModem extends AbstractTurtleUpgrade
{
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
            return new Vec3d(
                turtlePos.getX(),
                turtlePos.getY(),
                turtlePos.getZ()
            );
        }

        @Override
        public boolean equals( IPeripheral other )
        {
            return this == other || (other instanceof Peripheral && ((Peripheral) other).turtle == turtle);
        }
    }

    private boolean advanced;

    @Environment( EnvType.CLIENT )
    private ModelIdentifier m_leftOffModel;

    @Environment( EnvType.CLIENT )
    private ModelIdentifier m_rightOffModel;

    @Environment( EnvType.CLIENT )
    private ModelIdentifier m_leftOnModel;

    @Environment( EnvType.CLIENT )
    private ModelIdentifier m_rightOnModel;

    public TurtleModem( boolean advanced, Identifier id )
    {
        super(
            id, TurtleUpgradeType.Peripheral,
            advanced
                ? ComputerCraft.Blocks.wirelessModemAdvanced
                : ComputerCraft.Blocks.wirelessModemNormal
        );
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

    @Environment( EnvType.CLIENT )
    private void loadModelLocations()
    {
        if( m_leftOffModel == null )
        {
            if( advanced )
            {
                m_leftOffModel = new ModelIdentifier( "computercraft:turtle_modem_advanced_off_left", "inventory" );
                m_rightOffModel = new ModelIdentifier( "computercraft:turtle_modem_advanced_off_right", "inventory" );
                m_leftOnModel = new ModelIdentifier( "computercraft:turtle_modem_advanced_on_left", "inventory" );
                m_rightOnModel = new ModelIdentifier( "computercraft:turtle_modem_advanced_on_right", "inventory" );
            }
            else
            {
                m_leftOffModel = new ModelIdentifier( "computercraft:turtle_modem_normal_off_left", "inventory" );
                m_rightOffModel = new ModelIdentifier( "computercraft:turtle_modem_normal_off_right", "inventory" );
                m_leftOnModel = new ModelIdentifier( "computercraft:turtle_modem_normal_on_left", "inventory" );
                m_rightOnModel = new ModelIdentifier( "computercraft:turtle_modem_normal_on_right", "inventory" );
            }
        }
    }

    @Nonnull
    @Override
    @Environment( EnvType.CLIENT )
    public Pair<BakedModel, Matrix4f> getModel( ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        loadModelLocations();

        boolean active = false;
        if( turtle != null )
        {
            CompoundTag turtleNBT = turtle.getUpgradeNbtData( side );
            if( turtleNBT.containsKey( "active" ) )
            {
                active = turtleNBT.getBoolean( "active" );
            }
        }

        Matrix4f transform = null;
        BakedModelManager modelManager = MinecraftClient.getInstance().getItemRenderer().getModels().getModelManager();
        if( side == TurtleSide.Left )
        {
            return Pair.of(
                active ? modelManager.getModel( m_leftOnModel ) : modelManager.getModel( m_leftOffModel ),
                transform
            );
        }
        else
        {
            return Pair.of(
                active ? modelManager.getModel( m_rightOnModel ) : modelManager.getModel( m_rightOffModel ),
                transform
            );
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
                    turtle.getUpgradeNbtData( side ).putBoolean( "active", state.isOpen() );
                    turtle.updateUpgradeNBTData( side );
                }
            }
        }
    }
}
