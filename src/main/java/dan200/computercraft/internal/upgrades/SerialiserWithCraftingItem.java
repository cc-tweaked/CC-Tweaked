/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.internal.upgrades;

import com.google.gson.JsonObject;
import dan200.computercraft.api.upgrades.IUpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeSerialiser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nonnull;
import java.util.function.BiFunction;

/**
 * Simple serialiser which returns a constant upgrade with a custom crafting item.
 *
 * Do <strong>NOT</strong> directly reference this class. It exists for internal use by the API.
 *
 * @param <R> The serialiser for this upgrade category, either {@code TurtleUpgradeSerialiser<?>} or {@code PocketUpgradeSerialiser<?>}.
 * @param <T> The upgrade that this class can serialise and deserialise.
 */
public abstract class SerialiserWithCraftingItem<T extends IUpgradeBase, R extends UpgradeSerialiser<?, R>> extends ForgeRegistryEntry<R> implements UpgradeSerialiser<T, R>
{
    private final BiFunction<ResourceLocation, ItemStack, T> factory;

    protected SerialiserWithCraftingItem( BiFunction<ResourceLocation, ItemStack, T> factory )
    {
        this.factory = factory;
    }

    @Nonnull
    @Override
    public final T fromJson( @Nonnull ResourceLocation id, @Nonnull JsonObject object )
    {
        var item = GsonHelper.getAsItem( object, "item" );
        return factory.apply( id, new ItemStack( item ) );
    }

    @Nonnull
    @Override
    public final T fromNetwork( @Nonnull ResourceLocation id, @Nonnull FriendlyByteBuf buffer )
    {
        ItemStack item = buffer.readItem();
        return factory.apply( id, item );
    }

    @Override
    public final void toNetwork( @Nonnull FriendlyByteBuf buffer, @Nonnull T upgrade )
    {
        buffer.writeItem( upgrade.getCraftingItem() );
    }
}
