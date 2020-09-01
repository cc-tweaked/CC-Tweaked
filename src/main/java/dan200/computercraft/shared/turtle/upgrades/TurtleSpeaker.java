/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.AbstractTurtleUpgrade;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPeripheral;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class TurtleSpeaker extends AbstractTurtleUpgrade
{
    private static class Peripheral extends SpeakerPeripheral
    {
        ITurtleAccess turtle;

        Peripheral( ITurtleAccess turtle )
        {
            this.turtle = turtle;
        }

        @Override
        public World getWorld()
        {
            return turtle.getWorld();
        }

        @Override
        public Vec3d getPosition()
        {
            BlockPos pos = turtle.getPosition();
            return new Vec3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
        }

        @Override
        public boolean equals( IPeripheral other )
        {
            return this == other || (other instanceof Peripheral && turtle == ((Peripheral) other).turtle);
        }
    }

    @Environment(EnvType.CLIENT)
    private ModelIdentifier m_leftModel;

    @Environment(EnvType.CLIENT)
    private ModelIdentifier m_rightModel;

    public TurtleSpeaker( Identifier id )
    {
        super( id, TurtleUpgradeType.PERIPHERAL, ComputerCraftRegistry.ModBlocks.SPEAKER );
    }

    @Override
    public IPeripheral createPeripheral( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return new TurtleSpeaker.Peripheral( turtle );
    }

    @Environment(EnvType.CLIENT)
    private void loadModelLocations()
    {
        if( m_leftModel == null )
        {
            m_leftModel = new ModelIdentifier( "computercraft:turtle_speaker_upgrade_left", "inventory" );
            m_rightModel = new ModelIdentifier( "computercraft:turtle_speaker_upgrade_right", "inventory" );
        }
    }

    @Nonnull
    @Override
    @Environment(EnvType.CLIENT)
    public TransformedModel getModel( ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        loadModelLocations();
        return TransformedModel.of( side == TurtleSide.LEFT ? m_leftModel : m_rightModel );
    }

    @Override
    public void update( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide turtleSide )
    {
        IPeripheral turtlePeripheral = turtle.getPeripheral( turtleSide );
        if( turtlePeripheral instanceof Peripheral )
        {
            Peripheral peripheral = (Peripheral) turtlePeripheral;
            peripheral.update();
        }
    }
}
