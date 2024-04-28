// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.upgrades;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dan200.computercraft.impl.PlatformHelper;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A data generator/provider for turtle and pocket computer upgrades. This should not be extended directly, instead see
 * the other subclasses.
 *
 * @param <T> The base class of upgrades.
 */
public abstract class UpgradeDataProvider<T extends UpgradeBase> implements DataProvider {
    private final PackOutput output;
    private final String name;
    private final ResourceKey<Registry<T>> registryName;
    private final Codec<T> codec;

    private @Nullable Map<ResourceKey<T>, T> upgrades;

    @ApiStatus.Internal
    protected UpgradeDataProvider(PackOutput output, String name, ResourceKey<Registry<T>> registryName, Codec<T> codec) {
        this.output = output;
        this.name = name;
        this.registryName = registryName;
        this.codec = codec;
    }

    /**
     * Add a new upgrade.
     *
     * @param id      The ID of the upgrade to create.
     * @param upgrade The upgrade to add.
     * @return The constructed upgrade, ready to be passed off to {@link #addUpgrades(Consumer)}'s consumer.
     */
    protected final Upgrade<T> upgrade(ResourceLocation id, T upgrade) {
        return new Upgrade<>(id, upgrade, j -> {
        });
    }

    /**
     * Add all turtle or pocket computer upgrades.
     * <p>
     * <strong>Example usage:</strong>
     * <pre>{@code
     * protected void addUpgrades(Consumer<Upgrade<ITurtleUpgrade>> addUpgrade) {
     *     upgrade(new ResourceLocation("mymod", "speaker"), new TurtleSpeaker(new ItemStack(Items.NOTE_BLOCK))).add(addUpgrade);
     * }
     * }</pre>
     *
     * @param addUpgrade A callback used to register an upgrade.
     */
    protected abstract void addUpgrades(Consumer<Upgrade<T>> addUpgrade);

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        var base = output.createPathProvider(PackOutput.Target.DATA_PACK, registryName.location().getNamespace() + "/" + registryName.location().getPath());

        Map<ResourceKey<T>, T> upgrades = new LinkedHashMap<>();

        List<CompletableFuture<?>> futures = new ArrayList<>();
        addUpgrades(upgrade -> {
            var id = ResourceKey.create(registryName, upgrade.id);
            if (upgrades.containsKey(id)) throw new IllegalStateException("Duplicate upgrade " + upgrade.id);

            var json = (JsonObject) codec.encodeStart(JsonOps.INSTANCE, upgrade.upgrade).getOrThrow();
            upgrade.serialise.accept(json);

            futures.add(DataProvider.saveStable(cache, json, base.json(upgrade.id)));

            upgrades.put(id, upgrade.upgrade);
        });

        this.upgrades = Collections.unmodifiableMap(upgrades);

        return Util.sequenceFailFast(futures);
    }

    @Override
    public final String getName() {
        return name;
    }

    /**
     * Get all registered upgrades.
     *
     * @return The map of registered upgrades.
     */
    public Map<ResourceKey<T>, T> getGeneratedUpgrades() {
        var upgrades = this.upgrades;
        if (upgrades == null) throw new IllegalStateException("Upgrades are not available yet");
        return upgrades;
    }

    /**
     * A constructed upgrade instance, produced {@link #addUpgrades(Consumer)}.
     *
     * @param <T> The type of upgrade.
     */
    public static final class Upgrade<T extends UpgradeBase> {
        private final ResourceLocation id;
        private final T upgrade;
        private final Consumer<JsonObject> serialise;

        private Upgrade(ResourceLocation id, T upgrade, Consumer<JsonObject> serialise) {
            this.id = id;
            this.upgrade = upgrade;
            this.serialise = serialise;
        }

        /**
         * Convenience method for registering an upgrade.
         *
         * @param add The callback given to {@link #addUpgrades(Consumer)}
         */
        public void add(Consumer<Upgrade<T>> add) {
            add.accept(this);
        }

        /**
         * Return a new {@link Upgrade} which requires the given mod to be present.
         * <p>
         * This uses mod-loader-specific hooks (Forge's crafting conditions and Fabric's resource conditions). If using
         * this in a multi-loader setup, you must generate resources separately for the two loaders.
         *
         * @param modId The id of the mod.
         * @return A new upgrade instance.
         */
        public Upgrade<T> requireMod(String modId) {
            return new Upgrade<>(id, upgrade, json -> {
                PlatformHelper.get().addRequiredModCondition(json, modId);
                serialise.accept(json);
            });
        }
    }
}
