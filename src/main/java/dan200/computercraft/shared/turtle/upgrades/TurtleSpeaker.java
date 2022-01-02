/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.AbstractTurtleUpgrade;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import dan200.computercraft.shared.peripheral.speaker.UpgradeSpeakerPeripheral;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

public class TurtleSpeaker extends AbstractTurtleUpgrade
{
    @Environment( EnvType.CLIENT )
    private static class Models
    {
        private static ModelResourceLocation leftModel = new ModelResourceLocation( "computercraft:turtle_speaker_upgrade_left", "inventory" );
        private static ModelResourceLocation rightModel = new ModelResourceLocation( "computercraft:turtle_speaker_upgrade_right", "inventory" );
    }

    private static class Peripheral extends UpgradeSpeakerPeripheral
    {
        final ITurtleAccess turtle;

        Peripheral( ITurtleAccess turtle )
        {
            this.turtle = turtle;
        }

        @Override
        public Level getLevel()
        {
            return turtle.getLevel();
        }

        @Nonnull
        @Override
        public Vec3 getPosition()
        {
            BlockPos pos = turtle.getPosition();
            return new Vec3( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
        }

        @Override
        public boolean equals( IPeripheral other )
        {
            return this == other || (other instanceof Peripheral speaker && turtle == speaker.turtle);
        }
    }

    public TurtleSpeaker( ResourceLocation id, ItemStack item )
    {
        super( id, TurtleUpgradeType.PERIPHERAL, UpgradeSpeakerPeripheral.ADJECTIVE, item );
    }

    @Override
    public IPeripheral createPeripheral( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return new TurtleSpeaker.Peripheral( turtle );
    }

    @Nonnull
    @Override
    @Environment( EnvType.CLIENT )
    public TransformedModel getModel( ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return TransformedModel.of( side == TurtleSide.LEFT ? Models.leftModel : Models.rightModel );
    }

    @Override
    public void update( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide turtleSide )
    {
        IPeripheral peripheral = turtle.getPeripheral( turtleSide );
        if( peripheral instanceof Peripheral speaker ) speaker.update();
    }
}
