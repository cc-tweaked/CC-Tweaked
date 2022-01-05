/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.media;

import dan200.computercraft.api.filesystem.IMount;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents an item that can be placed in a disk drive and used by a Computer.
 *
 * Implement this interface on your {@link Item} class to allow it to be used in the drive. Alternatively, register
 * a {@link IMediaProvider}.
 */
public interface IMedia
{
    /**
     * Get a string representing the label of this item. Will be called via {@code disk.getLabel()} in lua.
     *
     * @param stack The {@link ItemStack} to inspect.
     * @return The label. ie: "Dan's Programs".
     */
    @Nullable
    String getLabel( @Nonnull ItemStack stack );

    /**
     * Set a string representing the label of this item. Will be called vi {@code disk.setLabel()} in lua.
     *
     * @param stack The {@link ItemStack} to modify.
     * @param label The string to set the label to.
     * @return true if the label was updated, false if the label may not be modified.
     */
    default boolean setLabel( @Nonnull ItemStack stack, @Nullable String label )
    {
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
    default String getAudioTitle( @Nonnull ItemStack stack )
    {
        return null;
    }

    /**
     * If this disk represents an item with audio (like a record), get the resource name of the audio track to play.
     *
     * @param stack The {@link ItemStack} to modify.
     * @return The name, or null if this item does not represent an item with audio.
     */
    @Nullable
    default SoundEvent getAudio( @Nonnull ItemStack stack )
    {
        return null;
    }

    /**
     * If this disk represents an item with data (like a floppy disk), get a mount representing it's contents. This will
     * be mounted onto the filesystem of the computer while the media is in the disk drive.
     *
     * @param stack The {@link ItemStack} to modify.
     * @param world The world in which the item and disk drive reside.
     * @return The mount, or null if this item does not represent an item with data. If the mount returned also
     * implements {@link dan200.computercraft.api.filesystem.IWritableMount}, it will mounted using mountWritable()
     * @see IMount
     * @see dan200.computercraft.api.filesystem.IWritableMount
     * @see dan200.computercraft.api.ComputerCraftAPI#createSaveDirMount(Level, String, long)
     * @see dan200.computercraft.api.ComputerCraftAPI#createResourceMount(String, String)
     */
    @Nullable
    default IMount createDataMount( @Nonnull ItemStack stack, @Nonnull Level world )
    {
        return null;
    }
}
