// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.turtle;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.turtle.upgrades.TurtleModem;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * A {@link TurtleUpgradeModeller} for modems, providing different models depending on if the modem is on/off.
 */
public class TurtleModemModeller implements TurtleUpgradeModeller<TurtleModem> {
    @Override
    public TransformedModel getModel(TurtleModem upgrade, @Nullable ITurtleAccess turtle, TurtleSide side, DataComponentPatch data) {
        var component = data.get(ModRegistry.DataComponents.ON.get());
        var active = component != null && component.isPresent() && component.get();

        var models = upgrade.advanced() ? ModemModels.ADVANCED : ModemModels.NORMAL;
        return side == TurtleSide.LEFT
            ? TransformedModel.of(active ? models.leftOnModel() : models.leftOffModel())
            : TransformedModel.of(active ? models.rightOnModel() : models.rightOffModel());
    }

    @Override
    public Stream<ResourceLocation> getDependencies() {
        return Stream.of(ModemModels.NORMAL, ModemModels.ADVANCED).flatMap(ModemModels::getDependencies);
    }

    private record ModemModels(
        ResourceLocation leftOffModel, ResourceLocation rightOffModel,
        ResourceLocation leftOnModel, ResourceLocation rightOnModel
    ) {
        private static final ModemModels NORMAL = create("normal");
        private static final ModemModels ADVANCED = create("advanced");

        public static ModemModels create(String type) {
            return new ModemModels(
                new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_modem_" + type + "_off_left"),
                new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_modem_" + type + "_off_right"),
                new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_modem_" + type + "_on_left"),
                new ResourceLocation(ComputerCraftAPI.MOD_ID, "block/turtle_modem_" + type + "_on_right")
            );
        }

        public Stream<ResourceLocation> getDependencies() {
            return Stream.of(leftOffModel, rightOffModel, leftOnModel, rightOnModel);
        }
    }
}
