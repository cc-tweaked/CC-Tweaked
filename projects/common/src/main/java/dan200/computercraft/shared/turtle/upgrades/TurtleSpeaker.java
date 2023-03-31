// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.AbstractTurtleUpgrade;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import dan200.computercraft.shared.peripheral.speaker.SpeakerPosition;
import dan200.computercraft.shared.peripheral.speaker.UpgradeSpeakerPeripheral;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class TurtleSpeaker extends AbstractTurtleUpgrade {
    private static class Peripheral extends UpgradeSpeakerPeripheral {
        final ITurtleAccess turtle;

        Peripheral(ITurtleAccess turtle) {
            this.turtle = turtle;
        }

        @Override
        public SpeakerPosition getPosition() {
            return SpeakerPosition.of(turtle.getLevel(), Vec3.atCenterOf(turtle.getPosition()));
        }

        @Override
        public boolean equals(@Nullable IPeripheral other) {
            return this == other || (other instanceof Peripheral speaker && turtle == speaker.turtle);
        }
    }

    public TurtleSpeaker(ResourceLocation id, ItemStack item) {
        super(id, TurtleUpgradeType.PERIPHERAL, UpgradeSpeakerPeripheral.ADJECTIVE, item);
    }

    @Override
    public IPeripheral createPeripheral(ITurtleAccess turtle, TurtleSide side) {
        return new TurtleSpeaker.Peripheral(turtle);
    }

    @Override
    public void update(ITurtleAccess turtle, TurtleSide turtleSide) {
        var peripheral = turtle.getPeripheral(turtleSide);
        if (peripheral instanceof Peripheral speaker) speaker.update();
    }
}
