// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.turtle;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.turtle.upgrades.TurtleModem;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * A {@link TurtleUpgradeModeller} for modems, providing different models depending on if the modem is on/off.
 */
public class TurtleModemModeller implements TurtleUpgradeModeller<TurtleModem> {
    private final ResourceLocation leftOffModel;
    private final ResourceLocation rightOffModel;
    private final ResourceLocation leftOnModel;
    private final ResourceLocation rightOnModel;

    public TurtleModemModeller(boolean advanced) {
        if (advanced) {
            leftOffModel = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_modem_advanced_off_left");
            rightOffModel = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_modem_advanced_off_right");
            leftOnModel = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_modem_advanced_on_left");
            rightOnModel = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_modem_advanced_on_right");
        } else {
            leftOffModel = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_modem_normal_off_left");
            rightOffModel = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_modem_normal_off_right");
            leftOnModel = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_modem_normal_on_left");
            rightOnModel = new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_modem_normal_on_right");
        }
    }

    @Override
    public TransformedModel getModel(TurtleModem upgrade, @Nullable ITurtleAccess turtle, TurtleSide side) {
        var active = false;
        if (turtle != null) {
            var turtleNBT = turtle.getUpgradeNBTData(side);
            active = turtleNBT.contains("active") && turtleNBT.getBoolean("active");
        }

        return side == TurtleSide.LEFT
            ? TransformedModel.of(active ? leftOnModel : leftOffModel)
            : TransformedModel.of(active ? rightOnModel : rightOffModel);
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return List.of(leftOffModel, rightOffModel, leftOnModel, rightOnModel);
    }
}
