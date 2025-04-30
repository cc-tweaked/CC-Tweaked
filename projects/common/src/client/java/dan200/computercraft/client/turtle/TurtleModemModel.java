// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.turtle;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.client.StandaloneModel;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModelViaStandalone;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.turtle.upgrades.TurtleModem;
import dan200.computercraft.shared.util.DataComponentUtil;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A {@link TurtleUpgradeModel} for modems, providing different models depending on if the modem is on/off.
 *
 * @param normal   The models for a normal wireless modem.
 * @param advanced The models for an advanced/ender modem.
 */
public record TurtleModemModel(
    ModemModels<StandaloneModel> normal, ModemModels<StandaloneModel> advanced
) implements TurtleUpgradeModelViaStandalone<TurtleModem> {
    public static final TurtleUpgradeModel.Unbaked<TurtleModem> UNBAKED = new Unbaked();

    @Override
    public StandaloneModel getModel(TurtleModem modem, TurtleSide side, DataComponentPatch data) {
        var active = DataComponentUtil.isPresent(data, ModRegistry.DataComponents.ON.get(), x -> x);

        var models = modem.advanced() ? advanced() : normal();
        return side == TurtleSide.LEFT
            ? (active ? models.leftOnModel() : models.leftOffModel())
            : (active ? models.rightOnModel() : models.rightOffModel());
    }

    private static final class Unbaked implements TurtleUpgradeModel.Unbaked<TurtleModem> {
        @Override
        public TurtleUpgradeModel<TurtleModem> bake(ModelBaker baker) {
            return new TurtleModemModel(
                ModemModels.NORMAL.map(x -> StandaloneModel.of(x, baker)),
                ModemModels.ADVANCED.map(x -> StandaloneModel.of(x, baker))
            );
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
            ModemModels.NORMAL.forEach(resolver::markDependency);
            ModemModels.ADVANCED.forEach(resolver::markDependency);
        }
    }

    private record ModemModels<T>(
        T leftOffModel, T rightOffModel,
        T leftOnModel, T rightOnModel
    ) {
        private static final ModemModels<ResourceLocation> NORMAL = create("normal");
        private static final ModemModels<ResourceLocation> ADVANCED = create("advanced");

        static ModemModels<ResourceLocation> create(String type) {
            return new ModemModels<>(
                ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_modem_" + type + "_off_left"),
                ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_modem_" + type + "_off_right"),
                ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_modem_" + type + "_on_left"),
                ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_modem_" + type + "_on_right")
            );
        }

        public void forEach(Consumer<T> out) {
            out.accept(leftOffModel());
            out.accept(rightOffModel);
            out.accept(leftOnModel());
            out.accept(rightOnModel());
        }

        public <U> ModemModels<U> map(Function<T, U> mapper) {
            return new ModemModels<>(
                mapper.apply(leftOffModel()), mapper.apply(rightOffModel()),
                mapper.apply(leftOnModel()), mapper.apply(rightOnModel())
            );
        }
    }
}
