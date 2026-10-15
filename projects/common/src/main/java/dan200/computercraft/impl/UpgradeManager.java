// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import com.google.gson.*;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.api.upgrades.UpgradeSerialiser;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages turtle and pocket computer upgrades.
 *
 * @param <R> The type of upgrade serialisers.
 * @param <T> The type of upgrade.
 * @see TurtleUpgrades
 * @see PocketUpgrades
 */
public final class UpgradeManager<R extends UpgradeSerialiser<? extends T>, T extends UpgradeBase> extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LoggerFactory.getLogger(UpgradeManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public record UpgradeWrapper<R extends UpgradeSerialiser<? extends T>, T extends UpgradeBase>(
        String id, T upgrade, R serialiser, String modId
    ) {
    }

    private final String kind;
    private final ResourceKey<Registry<R>> registry;

    private Map<String, UpgradeWrapper<R, T>> current = Map.of();
    private Map<T, UpgradeWrapper<R, T>> currentWrappers = Map.of();

    public UpgradeManager(String kind, String path, ResourceKey<Registry<R>> registry) {
        super(GSON, path);
        this.kind = kind;
        this.registry = registry;
    }

    @Nullable
    public T get(String id) {
        var wrapper = current.get(id);
        return wrapper == null ? null : wrapper.upgrade();
    }

    @Nullable
    public UpgradeWrapper<R, T> getWrapper(T upgrade) {
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
            var upgrade = wrapper.upgrade;
            if (upgrade.getCraftingIngredient().test(stack) && upgrade.isItemSuitable(stack)) {
                return UpgradeData.of(wrapper.upgrade, stack, wrapper.upgrade.getUpgradeData(stack));
            }
        }

        return null;
    }

    public Collection<T> getUpgrades() {
        return currentWrappers.keySet();
    }

    public Map<String, UpgradeWrapper<R, T>> getUpgradeWrappers() {
        return current;
    }

    public record UpgradeKey(String id, String item, String data) {
        public UpgradeKey(String id, String data) {
            this(id, id + "Item", data);
        }
    }

    public @Nullable UpgradeData<T> readUpgrade(CompoundTag tag, UpgradeKey key) {
        if (!tag.contains(key.id(), Tag.TAG_STRING)) return null;

        var upgrade = get(tag.getString(key.id()));
        if (upgrade == null) return null;

        var data = tag.getCompound(key.data());
        var item = tag.contains(key.item(), Tag.TAG_COMPOUND) ? ItemStack.of(tag.getCompound(key.item())) : upgrade.getUpgradeItem(data);
        return UpgradeData.of(upgrade, item, data);
    }

    public static <T extends UpgradeBase> void writeUpgrade(CompoundTag tag, UpgradeKey key, @Nullable UpgradeData<T> upgrade) {
        if (upgrade == null) {
            tag.remove(key.id());
            tag.remove(key.data());
            tag.remove(key.item());
        } else {
            tag.putString(key.id(), upgrade.upgrade().getUpgradeID().toString());
            tag.put(key.item(), upgrade.getUpgradeItem().save(new CompoundTag()));
            if (upgrade.data().isEmpty()) {
                tag.remove(key.data());
            } else {
                tag.put(key.data(), upgrade.data().copy());
            }
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> upgrades, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, UpgradeWrapper<R, T>> newUpgrades = new HashMap<>();
        for (var element : upgrades.entrySet()) {
            try {
                loadUpgrade(newUpgrades, element.getKey(), element.getValue());
            } catch (IllegalArgumentException | JsonParseException e) {
                LOG.error("Error loading {} {} from JSON file", kind, element.getKey(), e);
            }
        }

        current = Collections.unmodifiableMap(newUpgrades);
        currentWrappers = newUpgrades.values().stream().collect(Collectors.toUnmodifiableMap(UpgradeWrapper::upgrade, x -> x));
        LOG.info("Loaded {} {}s", current.size(), kind);
    }

    private void loadUpgrade(Map<String, UpgradeWrapper<R, T>> current, ResourceLocation id, JsonElement json) {
        var root = GsonHelper.convertToJsonObject(json, "top element");
        if (!PlatformHelper.get().shouldLoadResource(root)) return;

        var serialiserId = new ResourceLocation(GsonHelper.getAsString(root, "type"));
        var serialiser = PlatformHelper.get().tryGetRegistryObject(registry, serialiserId);
        if (serialiser == null) throw new JsonSyntaxException("Unknown upgrade type '" + serialiserId + "'");

        // TODO: Can we track which mod this resource came from and use that instead? It's theoretically possible,
        //  but maybe not ideal for datapacks.
        var modId = id.getNamespace();
        if (modId.equals("minecraft") || modId.isEmpty()) modId = ComputerCraftAPI.MOD_ID;

        var upgrade = serialiser.fromJson(id, root);
        if (!upgrade.getUpgradeID().equals(id)) {
            throw new IllegalArgumentException("Upgrade " + id + " from " + serialiser + " was incorrectly given id " + upgrade.getUpgradeID());
        }

        var result = new UpgradeWrapper<R, T>(id.toString(), upgrade, serialiser, modId);
        current.put(result.id(), result);
    }

    public void loadFromNetwork(Map<String, UpgradeWrapper<R, T>> newUpgrades) {
        current = Collections.unmodifiableMap(newUpgrades);
        currentWrappers = newUpgrades.values().stream().collect(Collectors.toUnmodifiableMap(UpgradeWrapper::upgrade, x -> x));
    }
}
