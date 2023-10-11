// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.upgrades;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.impl.PlatformHelper;
import dan200.computercraft.impl.upgrades.SerialiserWithCraftingItem;
import dan200.computercraft.impl.upgrades.SimpleSerialiser;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A data generator/provider for turtle and pocket computer upgrades. This should not be extended directly, instead see
 * the other subclasses.
 *
 * @param <T> The base class of upgrades.
 * @param <R> The upgrade serialiser to register for.
 */
public abstract class UpgradeDataProvider<T extends UpgradeBase, R extends UpgradeSerialiser<? extends T>> implements DataProvider {
    private final PackOutput output;
    private final String name;
    private final String folder;
    private final ResourceKey<Registry<R>> registry;

    private @Nullable List<T> upgrades;

    protected UpgradeDataProvider(PackOutput output, String name, String folder, ResourceKey<Registry<R>> registry) {
        this.output = output;
        this.name = name;
        this.folder = folder;
        this.registry = registry;
    }

    /**
     * Register an upgrade using a "simple" serialiser (e.g. {@link TurtleUpgradeSerialiser#simple(Function)}).
     *
     * @param id         The ID of the upgrade to create.
     * @param serialiser The simple serialiser.
     * @return The constructed upgrade, ready to be passed off to {@link #addUpgrades(Consumer)}'s consumer.
     */
    public final Upgrade<R> simple(ResourceLocation id, R serialiser) {
        if (!(serialiser instanceof SimpleSerialiser)) {
            throw new IllegalStateException(serialiser + " must be a simple() seriaiser.");
        }

        return new Upgrade<>(id, serialiser, s -> {
        });
    }

    /**
     * Register an upgrade using a "simple" serialiser (e.g. {@link TurtleUpgradeSerialiser#simple(Function)}).
     *
     * @param id         The ID of the upgrade to create.
     * @param serialiser The simple serialiser.
     * @param item       The crafting upgrade for this item.
     * @return The constructed upgrade, ready to be passed off to {@link #addUpgrades(Consumer)}'s consumer.
     */
    public final Upgrade<R> simpleWithCustomItem(ResourceLocation id, R serialiser, Item item) {
        if (!(serialiser instanceof SerialiserWithCraftingItem)) {
            throw new IllegalStateException(serialiser + " must be a simpleWithCustomItem() serialiser.");
        }

        return new Upgrade<>(id, serialiser, s ->
            s.addProperty("item", PlatformHelper.get().getRegistryKey(Registries.ITEM, item).toString())
        );
    }

    /**
     * Add all turtle or pocket computer upgrades.
     * <p>
     * <strong>Example usage:</strong>
     * <pre>{@code
     * protected void addUpgrades(Consumer<Upgrade<TurtleUpgradeSerialiser<?>>> addUpgrade) {
     *     simple(new ResourceLocation("mymod", "speaker"), SPEAKER_SERIALISER.get()).add(addUpgrade);
     * }
     * }</pre>
     *
     * @param addUpgrade A callback used to register an upgrade.
     */
    protected abstract void addUpgrades(Consumer<Upgrade<R>> addUpgrade);

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        var base = output.getOutputFolder().resolve("data");

        Set<ResourceLocation> seen = new HashSet<>();
        List<T> upgrades = new ArrayList<>();
        List<CompletableFuture<?>> futures = new ArrayList<>();
        addUpgrades(upgrade -> {
            if (!seen.add(upgrade.id())) throw new IllegalStateException("Duplicate upgrade " + upgrade.id());

            var json = new JsonObject();
            json.addProperty("type", PlatformHelper.get().getRegistryKey(registry, upgrade.serialiser()).toString());
            upgrade.serialise().accept(json);

            futures.add(DataProvider.saveStable(cache, json, base.resolve(upgrade.id().getNamespace() + "/" + folder + "/" + upgrade.id().getPath() + ".json")));

            try {
                var result = upgrade.serialiser().fromJson(upgrade.id(), json);
                upgrades.add(result);
            } catch (IllegalArgumentException | JsonParseException e) {
                LOGGER.error("Failed to parse {} {}", name, upgrade.id(), e);
            }
        });

        this.upgrades = Collections.unmodifiableList(upgrades);
        return Util.sequenceFailFast(futures);
    }

    @Override
    public final String getName() {
        return name;
    }

    public final R existingSerialiser(ResourceLocation id) {
        var result = PlatformHelper.get().getRegistryObject(registry, id);
        if (result == null) throw new IllegalArgumentException("No such serialiser " + registry);
        return result;
    }

    public List<T> getGeneratedUpgrades() {
        if (upgrades == null) throw new IllegalStateException("Upgrades have not been generated yet");
        return upgrades;
    }

    /**
     * A constructed upgrade instance, produced {@link #addUpgrades(Consumer)}.
     *
     * @param id         The ID for this upgrade.
     * @param serialiser The serialiser which reads and writes this upgrade.
     * @param serialise  Augment the generated JSON with additional fields.
     * @param <R>        The type of upgrade serialiser.
     */
    public record Upgrade<R extends UpgradeSerialiser<?>>(
        ResourceLocation id, R serialiser, Consumer<JsonObject> serialise
    ) {
        /**
         * Convenience method for registering an upgrade.
         *
         * @param add The callback given to {@link #addUpgrades(Consumer)}
         */
        public void add(Consumer<Upgrade<R>> add) {
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
        public Upgrade<R> requireMod(String modId) {
            return new Upgrade<>(id, serialiser, json -> {
                PlatformHelper.get().addRequiredModCondition(json, modId);
                serialise.accept(json);
            });
        }
    }
}
