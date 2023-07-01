// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemPeripheral;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class TurtleModem extends AbstractTurtleUpgrade {
    private static class Peripheral extends WirelessModemPeripheral {
        private final ITurtleAccess turtle;

        Peripheral(ITurtleAccess turtle, boolean advanced) {
            super(new ModemState(), advanced);
            this.turtle = turtle;
        }

        @Override
        public Level getLevel() {
            return turtle.getLevel();
        }

        @Override
        public Vec3 getPosition() {
            var turtlePos = turtle.getPosition();
            return new Vec3(
                turtlePos.getX(),
                turtlePos.getY(),
                turtlePos.getZ()
            );
        }

        @Override
        public boolean equals(@Nullable IPeripheral other) {
            return this == other || (other instanceof Peripheral modem && modem.turtle == turtle);
        }
    }

    private final boolean advanced;

    public TurtleModem(ResourceLocation id, ItemStack stack, boolean advanced) {
        super(id, TurtleUpgradeType.PERIPHERAL, advanced ? WirelessModemPeripheral.ADVANCED_ADJECTIVE : WirelessModemPeripheral.NORMAL_ADJECTIVE, stack);
        this.advanced = advanced;
    }

    @Override
    public IPeripheral createPeripheral(ITurtleAccess turtle, TurtleSide side) {
        return new Peripheral(turtle, advanced);
    }

    @Override
    public TurtleCommandResult useTool(ITurtleAccess turtle, TurtleSide side, TurtleVerb verb, Direction dir) {
        return TurtleCommandResult.failure();
    }

    @Override
    public void update(ITurtleAccess turtle, TurtleSide side) {
        // Advance the modem
        if (!turtle.getLevel().isClientSide) {
            var peripheral = turtle.getPeripheral(side);
            if (peripheral instanceof Peripheral modem) {
                var state = modem.getModemState();
                if (state.pollChanged()) {
                    turtle.getUpgradeNBTData(side).putBoolean("active", state.isOpen());
                    turtle.updateUpgradeNBTData(side);
                }
            }
        }
    }

    @Override
    public CompoundTag getPersistedData(CompoundTag upgradeData) {
        return new CompoundTag();
    }
}
