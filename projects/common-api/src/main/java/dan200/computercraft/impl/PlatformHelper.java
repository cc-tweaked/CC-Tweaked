// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import com.google.gson.JsonObject;
import dan200.computercraft.api.upgrades.UpgradeDataProvider;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

/**
 * Abstraction layer for Forge and Fabric. See implementations for more details.
 * <p>
 * Do <strong>NOT</strong> directly reference this class. It exists for internal use by the API.
 */
@ApiStatus.Internal
public interface PlatformHelper {
    /**
     * Get the current {@link PlatformHelper} instance.
     *
     * @return The current instance.
     */
    static PlatformHelper get() {
        var instance = Instance.INSTANCE;
        return instance == null ? Services.raise(PlatformHelper.class, Instance.ERROR) : instance;
    }

    /**
     * Get the unique ID for a registered object.
     *
     * @param registry The registry to look up this object in.
     * @param object   The object to look up.
     * @param <T>      The type of object the registry stores.
     * @return The registered object's ID.
     * @throws IllegalArgumentException If the registry or object are not registered.
     */
    <T> ResourceLocation getRegistryKey(ResourceKey<Registry<T>> registry, T object);

    /**
     * Look up an ID in a registry, returning the registered object.
     *
     * @param registry The registry to look up this object in.
     * @param id       The ID to look up.
     * @param <T>      The type of object the registry stores.
     * @return The resolved registry object.
     * @throws IllegalArgumentException If the registry or object are not registered.
     */
    <T> T getRegistryObject(ResourceKey<Registry<T>> registry, ResourceLocation id);

    /**
     * Get the subset of an {@link ItemStack}'s {@linkplain ItemStack#getTag() tag} which is synced to the client.
     *
     * @param item The stack.
     * @return The item's tag.
     */
    @Nullable
    default CompoundTag getShareTag(ItemStack item) {
        return item.getTag();
    }

    /**
     * Add a resource condition which requires a mod to be loaded. This should be used by data providers such as
     * {@link UpgradeDataProvider}.
     *
     * @param object The JSON object we're generating.
     * @param modId  The mod ID that we require.
     */
    void addRequiredModCondition(JsonObject object, String modId);

    final class Instance {
        static final @Nullable PlatformHelper INSTANCE;
        static final @Nullable Throwable ERROR;

        static {
            // We don't want class initialisation to fail here (as that results in confusing errors). Instead, capture
            // the error and rethrow it when accessing. This should be JITted away in the common case.
            var helper = Services.tryLoad(PlatformHelper.class);
            INSTANCE = helper.instance();
            ERROR = helper.error();
        }

        private Instance() {
        }
    }
}
