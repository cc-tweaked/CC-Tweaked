// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.api.upgrades.UpgradeSerialiser;
import dan200.computercraft.shared.platform.PlatformHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages turtle and pocket computer upgrades.
 *
 * @param <T> The type of upgrade.
 * @see TurtleUpgrades
 * @see PocketUpgrades
 */
public class UpgradeManager<T extends UpgradeBase> extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LoggerFactory.getLogger(UpgradeManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public record UpgradeWrapper<T extends UpgradeBase>(
        ResourceLocation id, T upgrade, UpgradeSerialiser<? extends T> serialiser, String modId
    ) {
    }

    private final String kind;
    private final ResourceKey<Registry<UpgradeSerialiser<? extends T>>> registry;

    private Map<ResourceLocation, UpgradeWrapper<T>> current = Map.of();
    private Map<T, UpgradeWrapper<T>> currentWrappers = Map.of();

    private final Codec<T> upgradeCodec = ResourceLocation.CODEC.flatXmap(
        x -> {
            var upgrade = get(x);
            return upgrade == null ? DataResult.error(() -> "Unknown upgrade " + x) : DataResult.success(upgrade);
        },
        x -> DataResult.success(x.getUpgradeID())
    );

    private final Codec<UpgradeData<T>> fullCodec = RecordCodecBuilder.create(i -> i.group(
        upgradeCodec.fieldOf("id").forGetter(UpgradeData::upgrade),
        DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(UpgradeData::data)
    ).apply(i, UpgradeData::new));

    private final Codec<UpgradeData<T>> codec = Codec.withAlternative(fullCodec, upgradeCodec, UpgradeData::ofDefault);

    private final StreamCodec<ByteBuf, T> upgradeStreamCodec = ResourceLocation.STREAM_CODEC.map(
        x -> {
            var upgrade = get(x);
            if (upgrade == null) throw new IllegalStateException("Unknown upgrade " + x);
            return upgrade;
        },
        UpgradeBase::getUpgradeID
    );

    private final StreamCodec<RegistryFriendlyByteBuf, UpgradeData<T>> streamCodec = StreamCodec.composite(
        upgradeStreamCodec, UpgradeData::upgrade,
        DataComponentPatch.STREAM_CODEC, UpgradeData::data,
        UpgradeData::new
    );

    public UpgradeManager(String kind, String path, ResourceKey<Registry<UpgradeSerialiser<? extends T>>> registry) {
        super(GSON, path);
        this.kind = kind;
        this.registry = registry;
    }

    @Nullable
    public T get(ResourceLocation id) {
        var wrapper = current.get(id);
        return wrapper == null ? null : wrapper.upgrade();
    }

    @Nullable
    public UpgradeWrapper<T> getWrapper(T upgrade) {
        return currentWrappers.get(upgrade);
    }

    @Nullable
    public String getOwner(T upgrade) {
        var wrapper = currentWrappers.get(upgrade);
        return wrapper != null ? wrapper.modId() : null;
    }

    @Nullable
    public UpgradeData<T> get(ItemStack stack) {
        if (stack.isEmpty()) return null;

        for (var wrapper : current.values()) {
            var craftingStack = wrapper.upgrade().getCraftingItem();
            if (!craftingStack.isEmpty() && craftingStack.getItem() == stack.getItem() && wrapper.upgrade().isItemSuitable(stack)) {
                return UpgradeData.of(wrapper.upgrade, wrapper.upgrade.getUpgradeData(stack));
            }
        }

        return null;
    }

    public Collection<T> getUpgrades() {
        return currentWrappers.keySet();
    }

    public Map<ResourceLocation, UpgradeWrapper<T>> getUpgradeWrappers() {
        return current;
    }

    public Codec<UpgradeData<T>> codec() {
        return codec;
    }

    public StreamCodec<RegistryFriendlyByteBuf, UpgradeData<T>> streamCodec() {
        return streamCodec;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> upgrades, ResourceManager manager, ProfilerFiller profiler) {
        var registry = RegistryHelper.getRegistry(this.registry);
        Map<ResourceLocation, UpgradeWrapper<T>> newUpgrades = new HashMap<>();
        for (var element : upgrades.entrySet()) {
            try {
                loadUpgrade(registry, newUpgrades, element.getKey(), element.getValue());
            } catch (IllegalArgumentException | JsonParseException e) {
                LOG.error("Error loading {} {} from JSON file", kind, element.getKey(), e);
            }
        }

        current = Collections.unmodifiableMap(newUpgrades);
        currentWrappers = newUpgrades.values().stream().collect(Collectors.toUnmodifiableMap(UpgradeWrapper::upgrade, x -> x));
        LOG.info("Loaded {} {}s", current.size(), kind);
    }

    private void loadUpgrade(Registry<UpgradeSerialiser<? extends T>> registry, Map<ResourceLocation, UpgradeWrapper<T>> current, ResourceLocation id, JsonElement json) {
        var root = GsonHelper.convertToJsonObject(json, "top element");
        if (!PlatformHelper.get().shouldLoadResource(root)) return;

        var serialiserId = new ResourceLocation(GsonHelper.getAsString(root, "type"));
        var serialiser = registry.get(serialiserId);
        if (serialiser == null) throw new JsonSyntaxException("Unknown upgrade type '" + serialiserId + "'");

        // TODO: Can we track which mod this resource came from and use that instead? It's theoretically possible,
        //  but maybe not ideal for datapacks.
        var modId = id.getNamespace();
        if (modId.equals("minecraft") || modId.isEmpty()) modId = ComputerCraftAPI.MOD_ID;

        var upgrade = serialiser.fromJson(id, root);
        if (!upgrade.getUpgradeID().equals(id)) {
            throw new IllegalArgumentException("Upgrade " + id + " from " + serialiser + " was incorrectly given id " + upgrade.getUpgradeID());
        }

        var result = new UpgradeWrapper<T>(id, upgrade, serialiser, modId);
        current.put(result.id(), result);
    }

    public void loadFromNetwork(Map<ResourceLocation, UpgradeWrapper<T>> newUpgrades) {
        current = Collections.unmodifiableMap(newUpgrades);
        currentWrappers = newUpgrades.values().stream().collect(Collectors.toUnmodifiableMap(UpgradeWrapper::upgrade, x -> x));
    }
}
