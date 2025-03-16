// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.media;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.shared.computer.items.AbstractComputerItem;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.config.ConfigSpec;
import dan200.computercraft.shared.media.items.DiskItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Media that provides a {@link Mount}.
 */
public final class MountMedia implements IMedia {
    /**
     * A {@link MountMedia} implementation for {@linkplain AbstractComputerItem computers}.
     */
    public static final IMedia COMPUTER = new MountMedia(
        "computer", s -> ((IComputerItem) s.getItem()).getComputerID(s), null, ConfigSpec.computerSpaceLimit
    );

    /**
     * A {@link MountMedia} implementation for {@linkplain DiskItem disks}.
     */
    public static final IMedia DISK = new MountMedia("disk", DiskItem::getDiskID, DiskItem::setDiskID, ConfigSpec.floppySpaceLimit);

    private final String subPath;
    private final ToIntFunction<ItemStack> getId;
    private final @Nullable IdSetter setId;
    private final Supplier<Integer> defaultCapacity;

    /**
     * Create a new {@link MountMedia}.
     *
     * @param subPath         The sub-path to expose the mount under, for instance {@code "computer"}.
     * @param getId           A function to get the item's ID.
     * @param setId           A function to set the item's ID. If not present, then mounts will not be created when the
     *                        item is placed in a drive.
     * @param defaultCapacity A function to get the default capacity of the stack.
     */
    public MountMedia(
        String subPath,
        ToIntFunction<ItemStack> getId,
        @Nullable IdSetter setId,
        Supplier<Integer> defaultCapacity
    ) {
        this.subPath = subPath;
        this.getId = getId;
        this.setId = setId;
        this.defaultCapacity = defaultCapacity;
    }

    @Override
    public @Nullable String getLabel(ItemStack stack) {
        return stack.hasCustomHoverName() ? stack.getHoverName().getString() : null;
    }

    @Override
    public boolean setLabel(ItemStack stack, @Nullable String label) {
        if (label != null) {
            stack.setHoverName(Component.literal(label));
        } else {
            stack.resetHoverName();
        }
        return true;
    }

    @Override
    public @Nullable Mount createDataMount(ItemStack stack, ServerLevel level) {
        var id = getId.applyAsInt(stack);
        if (id < 0) {
            if (setId == null) return null;
            id = ComputerCraftAPI.createUniqueNumberedSaveDir(level.getServer(), subPath);
            setId.set(stack, id);
        }
        return ComputerCraftAPI.createSaveDirMount(level.getServer(), subPath + "/" + id, defaultCapacity.get());
    }

    public interface IdSetter {
        void set(ItemStack stack, int id);
    }
}
