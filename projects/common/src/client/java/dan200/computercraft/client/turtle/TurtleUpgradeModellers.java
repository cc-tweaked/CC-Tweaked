// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.turtle;

import com.mojang.math.Transformation;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.UpgradeType;
import dan200.computercraft.shared.util.RegistryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * A registry of {@link TurtleUpgradeModeller}s.
 */
public final class TurtleUpgradeModellers {
    private static final TurtleUpgradeModeller<ITurtleUpgrade> NULL_TURTLE_MODELLER = (upgrade, turtle, side, data) ->
        new TransformedModel(Minecraft.getInstance().getModelManager().getMissingModel(), Transformation.identity());

    private static final Map<UpgradeType<? extends ITurtleUpgrade>, TurtleUpgradeModeller<?>> turtleModels = new ConcurrentHashMap<>();
    private static volatile boolean fetchedModels;

    private TurtleUpgradeModellers() {
    }

    public static <T extends ITurtleUpgrade> void register(UpgradeType<T> type, TurtleUpgradeModeller<T> modeller) {
        if (fetchedModels) {
            throw new IllegalStateException(String.format(
                "Turtle upgrade type %s must be registered before models are baked.",
                RegistryHelper.getKeyOrThrow(RegistryHelper.getRegistry(ITurtleUpgrade.typeRegistry()), type)
            ));
        }

        if (turtleModels.putIfAbsent(type, modeller) != null) {
            throw new IllegalStateException("Modeller already registered for serialiser");
        }
    }

    public static TransformedModel getModel(ITurtleUpgrade upgrade, ITurtleAccess access, TurtleSide side) {
        return getModeller(upgrade).getModel(upgrade, access, side, access.getUpgradeData(side));
    }

    public static TransformedModel getModel(ITurtleUpgrade upgrade, DataComponentPatch data, TurtleSide side) {
        return getModeller(upgrade).getModel(upgrade, null, side, data);
    }

    @SuppressWarnings("unchecked")
    private static <T extends ITurtleUpgrade> TurtleUpgradeModeller<T> getModeller(T upgrade) {
        var modeller = turtleModels.get(upgrade.getType());
        return (TurtleUpgradeModeller<T>) (modeller == null ? NULL_TURTLE_MODELLER : modeller);
    }

    public static Stream<ResourceLocation> getDependencies() {
        fetchedModels = true;
        return turtleModels.values().stream().flatMap(TurtleUpgradeModeller::getDependencies);
    }
}
