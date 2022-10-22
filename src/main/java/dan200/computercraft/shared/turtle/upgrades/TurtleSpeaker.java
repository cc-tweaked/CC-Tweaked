/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.AbstractTurtleUpgrade;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import dan200.computercraft.shared.peripheral.speaker.UpgradeSpeakerPeripheral;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

public class TurtleSpeaker extends AbstractTurtleUpgrade
{
    private static final ResourceLocation leftModel = new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_speaker_left" );
    private static final ResourceLocation rightModel = new ResourceLocation( ComputerCraft.MOD_ID, "block/turtle_speaker_right" );

    private static class Peripheral extends UpgradeSpeakerPeripheral
    {
        ITurtleAccess turtle;

        Peripheral( ITurtleAccess turtle )
        {
            this.turtle = turtle;
        }

        @Nonnull
        @Override
        public SpeakerPosition getPosition()
        {
            return SpeakerPosition.of( turtle.getWorld(), Vector3d.atCenterOf( turtle.getPosition() ) );
        }

        @Override
        public boolean equals( IPeripheral other )
        {
            return this == other || (other instanceof Peripheral && turtle == ((Peripheral) other).turtle);
        }
    }

    public TurtleSpeaker( ResourceLocation id )
    {
        super( id, TurtleUpgradeType.PERIPHERAL, Registry.ModBlocks.SPEAKER );
    }

    @Override
    public IPeripheral createPeripheral( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return new TurtleSpeaker.Peripheral( turtle );
    }

    @Nonnull
    @Override
    @OnlyIn( Dist.CLIENT )
    public TransformedModel getModel( ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return TransformedModel.of( side == TurtleSide.LEFT ? leftModel : rightModel );
    }

    @Override
    public void update( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide turtleSide )
    {
        IPeripheral turtlePeripheral = turtle.getPeripheral( turtleSide );
        if( turtlePeripheral instanceof Peripheral ) ((Peripheral) turtlePeripheral).update();
    }
}
