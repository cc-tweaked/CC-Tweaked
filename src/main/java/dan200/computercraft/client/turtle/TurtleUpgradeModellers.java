/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.turtle;

import com.mojang.math.Transformation;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.shared.TurtleUpgrades;
import dan200.computercraft.shared.UpgradeManager;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class TurtleUpgradeModellers {
    private static final TurtleUpgradeModeller<ITurtleUpgrade> NULL_TURTLE_MODELLER = (upgrade, turtle, side) ->
        new TransformedModel(Minecraft.getInstance().getModelManager().getMissingModel(), Transformation.identity());

    private static final Map<TurtleUpgradeSerialiser<?>, TurtleUpgradeModeller<?>> turtleModels = new ConcurrentHashMap<>();

    /**
     * In order to avoid a double lookup of {@link ITurtleUpgrade} to {@link UpgradeManager.UpgradeWrapper} to
     * {@link TurtleUpgradeModeller}, we maintain a cache here.
     * <p>
     * Turtle upgrades may be removed as part of datapack reloads, so we use a weak map to avoid the memory leak.
     */
    private static final WeakHashMap<ITurtleUpgrade, TurtleUpgradeModeller<?>> modelCache = new WeakHashMap<>();

    private TurtleUpgradeModellers() {
    }

    public static <T extends ITurtleUpgrade> void register(TurtleUpgradeSerialiser<T> serialiser, TurtleUpgradeModeller<T> modeller) {
        synchronized (turtleModels) {
            if (turtleModels.containsKey(serialiser)) {
                throw new IllegalStateException("Modeller already registered for serialiser");
            }

            turtleModels.put(serialiser, modeller);
        }
    }

    public static TransformedModel getModel(ITurtleUpgrade upgrade, @Nullable ITurtleAccess access, TurtleSide side) {
        @SuppressWarnings("unchecked")
        var modeller = (TurtleUpgradeModeller<ITurtleUpgrade>) modelCache.computeIfAbsent(upgrade, TurtleUpgradeModellers::getModeller);
        return modeller.getModel(upgrade, access, side);
    }

    private static TurtleUpgradeModeller<?> getModeller(ITurtleUpgrade upgradeA) {
        var wrapper = TurtleUpgrades.instance().getWrapper(upgradeA);
        if (wrapper == null) return NULL_TURTLE_MODELLER;

        var modeller = turtleModels.get(wrapper.serialiser());
        return modeller == null ? NULL_TURTLE_MODELLER : modeller;
    }
}
