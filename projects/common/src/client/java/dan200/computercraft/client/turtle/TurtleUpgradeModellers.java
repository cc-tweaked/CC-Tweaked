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
import dan200.computercraft.impl.RegistryHelper;
import dan200.computercraft.impl.UpgradeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.WeakHashMap;
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

    /**
     * In order to avoid a double lookup of {@link ITurtleUpgrade} to {@link UpgradeManager.UpgradeWrapper} to
     * {@link TurtleUpgradeModeller}, we maintain a cache here.
     * <p>
     * Turtle upgrades may be removed as part of datapack reloads, so we use a weak map to avoid the memory leak.
     */
    private static final WeakHashMap<ITurtleUpgrade, TurtleUpgradeModeller<?>> modelCache = new WeakHashMap<>();

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
        @SuppressWarnings("unchecked")
        var modeller = (TurtleUpgradeModeller<ITurtleUpgrade>) modelCache.computeIfAbsent(upgrade, TurtleUpgradeModellers::getModeller);
        return modeller.getModel(upgrade, access, side, access.getUpgradeData(side));
    }

    public static TransformedModel getModel(ITurtleUpgrade upgrade, DataComponentPatch data, TurtleSide side) {
        @SuppressWarnings("unchecked")
        var modeller = (TurtleUpgradeModeller<ITurtleUpgrade>) modelCache.computeIfAbsent(upgrade, TurtleUpgradeModellers::getModeller);
        return modeller.getModel(upgrade, null, side, data);
    }

    private static TurtleUpgradeModeller<?> getModeller(ITurtleUpgrade upgrade) {
        var modeller = turtleModels.get(upgrade.getType());
        return modeller == null ? NULL_TURTLE_MODELLER : modeller;
    }

    public static Stream<ResourceLocation> getDependencies() {
        fetchedModels = true;
        return turtleModels.values().stream().flatMap(TurtleUpgradeModeller::getDependencies);
    }
}
