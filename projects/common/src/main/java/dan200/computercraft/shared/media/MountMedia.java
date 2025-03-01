// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.media;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.items.ComputerItem;
import dan200.computercraft.shared.config.ConfigSpec;
import dan200.computercraft.shared.media.items.DiskItem;
import dan200.computercraft.shared.util.DataComponentUtil;
import dan200.computercraft.shared.util.NonNegativeId;
import dan200.computercraft.shared.util.StorageCapacity;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Media that provides a {@link Mount}.
 */
public final class MountMedia implements IMedia {
    /**
     * A {@link MountMedia} implementation for {@linkplain ComputerItem computers}.
     */
    public static final IMedia COMPUTER = new MountMedia("computer", ModRegistry.DataComponents.COMPUTER_ID, false, ConfigSpec.computerSpaceLimit);

    /**
     * A {@link MountMedia} implementation for {@linkplain DiskItem disks}.
     */
    public static final IMedia DISK = new MountMedia("disk", ModRegistry.DataComponents.DISK_ID, true, ConfigSpec.floppySpaceLimit);

    private final String subPath;
    private final Supplier<DataComponentType<NonNegativeId>> id;
    private final boolean createId;
    private final Supplier<Integer> defaultCapacity;

    /**
     * Create a new {@link MountMedia}.
     *
     * @param subPath         The sub-path to expose the mount under, for instance {@code "computer"}.
     * @param id              The component that stores the ID.
     * @param createId        Whether to allocate a new ID if the item does not yet have one.
     * @param defaultCapacity A function to get the default capacity of the stack.
     */
    public MountMedia(
        String subPath,
        Supplier<DataComponentType<NonNegativeId>> id,
        boolean createId,
        Supplier<Integer> defaultCapacity
    ) {
        this.subPath = subPath;
        this.id = id;
        this.createId = createId;
        this.defaultCapacity = defaultCapacity;
    }

    @Override
    public @Nullable String getLabel(HolderLookup.Provider registries, ItemStack stack) {
        return DataComponentUtil.getCustomName(stack);
    }

    @Override
    public boolean setLabel(ItemStack stack, @Nullable String label) {
        DataComponentUtil.setCustomName(stack, label);
        return true;
    }

    @Override
    public @Nullable Mount createDataMount(ItemStack stack, ServerLevel level) {
        var id = createId
            ? NonNegativeId.getOrCreate(level.getServer(), stack, this.id.get(), subPath)
            : NonNegativeId.getId(stack.get(this.id.get()));
        if (id < 0) return null;

        var capacity = StorageCapacity.getOrDefault(stack.get(ModRegistry.DataComponents.STORAGE_CAPACITY.get()), defaultCapacity);
        return ComputerCraftAPI.createSaveDirMount(level.getServer(), subPath + "/" + id, capacity);
    }
}
