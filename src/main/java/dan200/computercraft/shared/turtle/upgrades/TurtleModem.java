/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import javax.annotation.Nonnull;
import javax.vecmath.Matrix4f;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.AbstractTurtleUpgrade;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import dan200.computercraft.api.turtle.TurtleVerb;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemPeripheral;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class TurtleModem extends AbstractTurtleUpgrade {
    private boolean advanced;
    @Environment (EnvType.CLIENT) private ModelIdentifier m_leftOffModel;
    @Environment (EnvType.CLIENT) private ModelIdentifier m_rightOffModel;
    @Environment (EnvType.CLIENT) private ModelIdentifier m_leftOnModel;
    @Environment (EnvType.CLIENT) private ModelIdentifier m_rightOnModel;

    public TurtleModem(boolean advanced, Identifier id) {
        super(id, TurtleUpgradeType.Peripheral, advanced ? ComputerCraft.Blocks.wirelessModemAdvanced : ComputerCraft.Blocks.wirelessModemNormal);
        this.advanced = advanced;
    }

    @Override
    public IPeripheral createPeripheral(@Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side) {
        return new Peripheral(turtle, this.advanced);
    }

    @Nonnull
    @Override
    public TurtleCommandResult useTool(@Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side, @Nonnull TurtleVerb verb, @Nonnull Direction dir) {
        return TurtleCommandResult.failure();
    }

    @Nonnull
    @Override
    @Environment (EnvType.CLIENT)
    public Pair<BakedModel, Matrix4f> getModel(ITurtleAccess turtle, @Nonnull TurtleSide side) {
        this.loadModelLocations();

        boolean active = false;
        if (turtle != null) {
            CompoundTag turtleNBT = turtle.getUpgradeNBTData(side);
            if (turtleNBT.contains("active")) {
                active = turtleNBT.getBoolean("active");
            }
        }

        Matrix4f transform = null;
        BakedModelManager modelManager = MinecraftClient.getInstance()
                                                        .getItemRenderer()
                                                        .getModels()
                                                        .getModelManager();
        if (side == TurtleSide.Left) {
            return Pair.of(active ? modelManager.getModel(this.m_leftOnModel) : modelManager.getModel(this.m_leftOffModel), transform);
        } else {
            return Pair.of(active ? modelManager.getModel(this.m_rightOnModel) : modelManager.getModel(this.m_rightOffModel), transform);
        }
    }

    @Environment (EnvType.CLIENT)
    private void loadModelLocations() {
        if (this.m_leftOffModel == null) {
            if (this.advanced) {
                this.m_leftOffModel = new ModelIdentifier("computercraft:turtle_modem_advanced_off_left", "inventory");
                this.m_rightOffModel = new ModelIdentifier("computercraft:turtle_modem_advanced_off_right", "inventory");
                this.m_leftOnModel = new ModelIdentifier("computercraft:turtle_modem_advanced_on_left", "inventory");
                this.m_rightOnModel = new ModelIdentifier("computercraft:turtle_modem_advanced_on_right", "inventory");
            } else {
                this.m_leftOffModel = new ModelIdentifier("computercraft:turtle_modem_normal_off_left", "inventory");
                this.m_rightOffModel = new ModelIdentifier("computercraft:turtle_modem_normal_off_right", "inventory");
                this.m_leftOnModel = new ModelIdentifier("computercraft:turtle_modem_normal_on_left", "inventory");
                this.m_rightOnModel = new ModelIdentifier("computercraft:turtle_modem_normal_on_right", "inventory");
            }
        }
    }

    @Override
    public void update(@Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side) {
        // Advance the modem
        if (!turtle.getWorld().isClient) {
            IPeripheral peripheral = turtle.getPeripheral(side);
            if (peripheral instanceof Peripheral) {
                ModemState state = ((Peripheral) peripheral).getModemState();
                if (state.pollChanged()) {
                    turtle.getUpgradeNBTData(side)
                          .putBoolean("active", state.isOpen());
                    turtle.updateUpgradeNBTData(side);
                }
            }
        }
    }

    private static class Peripheral extends WirelessModemPeripheral {
        private final ITurtleAccess turtle;

        Peripheral(ITurtleAccess turtle, boolean advanced) {
            super(new ModemState(), advanced);
            this.turtle = turtle;
        }

        @Nonnull
        @Override
        public World getWorld() {
            return this.turtle.getWorld();
        }

        @Nonnull
        @Override
        public Vec3d getPosition() {
            BlockPos turtlePos = this.turtle.getPosition();
            return new Vec3d(turtlePos.getX(), turtlePos.getY(), turtlePos.getZ());
        }

        @Override
        public boolean equals(IPeripheral other) {
            return this == other || (other instanceof Peripheral && ((Peripheral) other).turtle == this.turtle);
        }
    }
}
