// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.media;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Represents an item that can be placed in a disk drive and used by a Computer.
 * <p>
 * Implement this interface on your {@link Item} class to allow it to be used in the drive. Alternatively, register
 * a {@link MediaProvider}.
 */
public interface IMedia {
    /**
     * Get a string representing the label of this item. Will be called via {@code disk.getLabel()} in lua.
     *
     * @param stack The {@link ItemStack} to inspect.
     * @return The label. ie: "Dan's Programs".
     */
    @Nullable
    String getLabel(ItemStack stack);

    /**
     * Set a string representing the label of this item. Will be called vi {@code disk.setLabel()} in lua.
     *
     * @param stack The {@link ItemStack} to modify.
     * @param label The string to set the label to.
     * @return true if the label was updated, false if the label may not be modified.
     */
    default boolean setLabel(ItemStack stack, @Nullable String label) {
        return false;
    }

    /**
     * If this disk represents an item with audio (like a record), get the readable name of the audio track. ie:
     * "Jonathan Coulton - Still Alive"
     *
     * @param stack The {@link ItemStack} to modify.
     * @return The name, or null if this item does not represent an item with audio.
     */
    @Nullable
    default String getAudioTitle(ItemStack stack) {
        return null;
    }

    /**
     * If this disk represents an item with audio (like a record), get the resource name of the audio track to play.
     *
     * @param stack The {@link ItemStack} to modify.
     * @return The name, or null if this item does not represent an item with audio.
     */
    @Nullable
    default SoundEvent getAudio(ItemStack stack) {
        return null;
    }

    /**
     * If this disk represents an item with data (like a floppy disk), get a mount representing it's contents. This will
     * be mounted onto the filesystem of the computer while the media is in the disk drive.
     *
     * @param stack The {@link ItemStack} to modify.
     * @param level The world in which the item and disk drive reside.
     * @return The mount, or null if this item does not represent an item with data. If the mount returned also
     * implements {@link WritableMount}, it will mounted using mountWritable()
     * @see Mount
     * @see WritableMount
     * @see ComputerCraftAPI#createSaveDirMount(MinecraftServer, String, long)
     * @see ComputerCraftAPI#createResourceMount(MinecraftServer, String, String)
     */
    @Nullable
    default Mount createDataMount(ItemStack stack, ServerLevel level) {
        return null;
    }
}
