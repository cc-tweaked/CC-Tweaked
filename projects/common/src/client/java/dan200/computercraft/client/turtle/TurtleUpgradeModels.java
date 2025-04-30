// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.turtle;

import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.upgrades.UpgradeType;
import dan200.computercraft.client.platform.ClientPlatformHelper;
import dan200.computercraft.client.platform.ModelKey;
import dan200.computercraft.shared.util.RegistryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.MissingBlockModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * A registry of {@link TurtleUpgradeModel}s.
 */
public final class TurtleUpgradeModels {
    private static final Object fetchedLock = new Object();
    private static volatile boolean fetchedModels;

    private static final Map<ModelKey<? extends TurtleUpgradeModel<?>>, TurtleUpgradeModel.Unbaked<?>> unbaked = new ConcurrentHashMap<>();
    private static final Map<UpgradeType<? extends ITurtleUpgrade>, ModelKey<? extends TurtleUpgradeModel<?>>> modelKeys = new ConcurrentHashMap<>();
    public static final ModelKey<TurtleUpgradeModel<ITurtleUpgrade>> missingModelKey = ClientPlatformHelper.get().createModelKey(
        MissingBlockModel.LOCATION,
        () -> "Missing turtle model"
    );

    private TurtleUpgradeModels() {
    }

    public static <T extends ITurtleUpgrade> void register(UpgradeType<T> type, TurtleUpgradeModel.Unbaked<? super T> modeller) {
        if (fetchedModels) {
            throw new IllegalStateException(String.format(
                "Turtle upgrade type %s must be registered before models are baked.",
                RegistryHelper.getKeyOrThrow(RegistryHelper.getRegistry(ITurtleUpgrade.typeRegistry()), type)
            ));
        }

        if (unbaked.putIfAbsent(getModelKey(type), modeller) != null) {
            throw new IllegalStateException("Modeller already registered for serialiser");
        }
    }

    public static void fetch(Runnable action) {
        if (fetchedModels) return;
        synchronized (fetchedLock) {
            if (fetchedModels) return;
            action.run();
            fetchedModels = true;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends ITurtleUpgrade> ModelKey<TurtleUpgradeModel<? super T>> getModelKey(UpgradeType<T> type) {
        return (ModelKey<TurtleUpgradeModel<? super T>>) modelKeys.computeIfAbsent(type, t -> {
            var id = RegistryHelper.getKeyOrThrow(RegistryHelper.getRegistry(ITurtleUpgrade.typeRegistry()), t);
            return ClientPlatformHelper.get().createModelKey(
                RegistryHelper.getKeyOrThrow(RegistryHelper.getRegistry(ITurtleUpgrade.typeRegistry()), t),
                () -> "Turtle upgrade " + id
            );
        });
    }

    public static <T extends ITurtleUpgrade> TurtleUpgradeModel<? super T> getModeller(T upgrade) {
        var modelManager = Minecraft.getInstance().getModelManager();

        @SuppressWarnings("unchecked")
        var model = getModelKey((UpgradeType<T>) upgrade.getType()).get(modelManager);
        if (model != null) return model;

        var missing = missingModelKey.get(modelManager);
        if (missing == null) throw new IllegalStateException("Rendering turtles before models are baked");
        return missing;
    }

    public static void bake(BiConsumer<ModelKey<? extends TurtleUpgradeModel<?>>, TurtleUpgradeModel.Unbaked<?>> baker) {
        unbaked.forEach(baker);
        baker.accept(missingModelKey, TurtleUpgradeModel.sided(MissingBlockModel.LOCATION, MissingBlockModel.LOCATION));
    }
}
