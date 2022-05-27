/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.detail;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An item detail provider for {@link ItemStack}'s whose {@link Item} has a specific type.
 *
 * @param <T> The type the stack's item must have.
 */
public abstract class BasicItemDetailProvider<T> implements IDetailProvider<ItemStack>
{
    private final Class<T> itemType;
    private final String namespace;

    /**
     * Create a new item detail provider. Meta will be inserted into a new sub-map named as per {@code namespace}.
     *
     * @param itemType  The type the stack's item must have.
     * @param namespace The namespace to use for this provider.
     */
    public BasicItemDetailProvider( String namespace, @Nonnull Class<T> itemType )
    {
        Objects.requireNonNull( itemType );
        this.itemType = itemType;
        this.namespace = namespace;
    }

    /**
     * Create a new item detail provider. Meta will be inserted directly into the results.
     *
     * @param itemType The type the stack's item must have.
     */
    public BasicItemDetailProvider( @Nonnull Class<T> itemType )
    {
        this( null, itemType );
    }

    /**
     * Provide additional details for the given {@link Item} and {@link ItemStack}. This method is called by
     * {@code turtle.getItemDetail()}. New properties should be added to the given {@link Map}, {@code data}.
     *
     * This method is always called on the server thread, so it is safe to interact with the world here, but you should
     * take care to avoid long blocking operations as this will stall the server and other computers.
     *
     * @param data  The full details to be returned for this item stack. New properties should be added to this map.
     * @param stack The item stack to provide details for.
     * @param item  The item to provide details for.
     */
    public abstract void provideDetails( @Nonnull Map<? super String, Object> data, @Nonnull ItemStack stack,
                                         @Nonnull T item );

    @Override
    public void provideDetails( @Nonnull Map<? super String, Object> data, @Nonnull ItemStack stack )
    {
        Item item = stack.getItem();
        if( !itemType.isInstance( item ) ) return;

        // If `namespace` is specified, insert into a new data map instead of the existing one.
        Map<? super String, Object> child = namespace == null ? data : new HashMap<>();

        provideDetails( child, stack, itemType.cast( item ) );

        if( namespace != null )
        {
            data.put( namespace, child );
        }
    }
}
