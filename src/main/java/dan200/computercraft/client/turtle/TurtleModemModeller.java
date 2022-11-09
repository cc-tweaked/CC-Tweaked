/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.turtle;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.turtle.upgrades.TurtleModem;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class TurtleModemModeller implements TurtleUpgradeModeller<TurtleModem> {
    private final ResourceLocation leftOffModel;
    private final ResourceLocation rightOffModel;
    private final ResourceLocation leftOnModel;
    private final ResourceLocation rightOnModel;

    public TurtleModemModeller(boolean advanced) {
        if (advanced) {
            leftOffModel = new ResourceLocation(ComputerCraft.MOD_ID, "block/turtle_modem_advanced_off_left");
            rightOffModel = new ResourceLocation(ComputerCraft.MOD_ID, "block/turtle_modem_advanced_off_right");
            leftOnModel = new ResourceLocation(ComputerCraft.MOD_ID, "block/turtle_modem_advanced_on_left");
            rightOnModel = new ResourceLocation(ComputerCraft.MOD_ID, "block/turtle_modem_advanced_on_right");
        } else {
            leftOffModel = new ResourceLocation(ComputerCraft.MOD_ID, "block/turtle_modem_normal_off_left");
            rightOffModel = new ResourceLocation(ComputerCraft.MOD_ID, "block/turtle_modem_normal_off_right");
            leftOnModel = new ResourceLocation(ComputerCraft.MOD_ID, "block/turtle_modem_normal_on_left");
            rightOnModel = new ResourceLocation(ComputerCraft.MOD_ID, "block/turtle_modem_normal_on_right");
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
}
