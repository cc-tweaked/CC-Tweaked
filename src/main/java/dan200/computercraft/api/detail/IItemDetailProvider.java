/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.detail;

import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * This interface is used to augment the {@code turtle.getItemDetail()} function, to provide more details about a
 * given item stack.
 *
 * @see dan200.computercraft.api.ComputerCraftAPI#registerDetailProvider(IDetailProvider)
 */
@FunctionalInterface
public interface IItemDetailProvider extends IDetailProvider<ItemStack>
{
    /**
     * Provide additional details for the given {@link ItemStack}. This method is called by
     * {@code turtle.getItemDetail()}. New properties should be added to the given {@link Map}, {@code data}.
     *
     * This method is always called on the server thread, so it is safe to interact with the world here, but you should
     * take care to avoid long blocking operations as this will stall the server and other computers.
     *
     * @param data  The full details to be returned for this item stack. New properties should be added to this map.
     * @param stack The item stack to provide details for.
     */
    void provideDetails( @Nonnull Map<? super String, Object> data, @Nonnull ItemStack stack );
}
