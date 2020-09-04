/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import javax.annotation.Nonnull;

import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.AbstractTurtleUpgrade;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPeripheral;

import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class TurtleSpeaker extends AbstractTurtleUpgrade {
    @Environment (EnvType.CLIENT) private ModelIdentifier m_leftModel;
    @Environment (EnvType.CLIENT) private ModelIdentifier m_rightModel;

    public TurtleSpeaker(Identifier id) {
        super(id, TurtleUpgradeType.PERIPHERAL, ComputerCraftRegistry.ModBlocks.SPEAKER);
    }

    @Override
    public IPeripheral createPeripheral(@Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side) {
        return new TurtleSpeaker.Peripheral(turtle);
    }

    @Nonnull
    @Override
    @Environment (EnvType.CLIENT)
    public TransformedModel getModel(ITurtleAccess turtle, @Nonnull TurtleSide side) {
        this.loadModelLocations();
        return TransformedModel.of(side == TurtleSide.LEFT ? this.m_leftModel : this.m_rightModel);
    }

    @Environment (EnvType.CLIENT)
    private void loadModelLocations() {
        if (this.m_leftModel == null) {
            this.m_leftModel = new ModelIdentifier("computercraft:turtle_speaker_upgrade_left", "inventory");
            this.m_rightModel = new ModelIdentifier("computercraft:turtle_speaker_upgrade_right", "inventory");
        }
    }

    @Override
    public void update(@Nonnull ITurtleAccess turtle, @Nonnull TurtleSide turtleSide) {
        IPeripheral turtlePeripheral = turtle.getPeripheral(turtleSide);
        if (turtlePeripheral instanceof Peripheral) {
            Peripheral peripheral = (Peripheral) turtlePeripheral;
            peripheral.update();
        }
    }

    private static class Peripheral extends SpeakerPeripheral {
        ITurtleAccess turtle;

        Peripheral(ITurtleAccess turtle) {
            this.turtle = turtle;
        }

        @Override
        public World getWorld() {
            return this.turtle.getWorld();
        }

        @Override
        public Vec3d getPosition() {
            BlockPos pos = this.turtle.getPosition();
            return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }

        @Override
        public boolean equals(IPeripheral other) {
            return this == other || (other instanceof Peripheral && this.turtle == ((Peripheral) other).turtle);
        }
    }
}
