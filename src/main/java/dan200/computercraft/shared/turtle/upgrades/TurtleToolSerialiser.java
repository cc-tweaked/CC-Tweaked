/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import com.google.gson.JsonObject;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.api.upgrades.IUpgradeBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class TurtleToolSerialiser<T extends TurtleTool> extends TurtleUpgradeSerialiser.Base<T>
{
    private final Factory<T> factory;

    public TurtleToolSerialiser( Factory<T> factory )
    {
        this.factory = factory;
    }

    public static <T extends TurtleTool> Supplier<TurtleUpgradeSerialiser<T>> make( Factory<T> factory )
    {
        return () -> new TurtleToolSerialiser<>( factory );
    }

    @Nonnull
    @Override
    public T fromJson( @Nonnull ResourceLocation id, @Nonnull JsonObject object )
    {
        var adjective = GsonHelper.getAsString( object, "adjective", IUpgradeBase.getDefaultAdjective( id ) );
        var toolItem = GsonHelper.getAsItem( object, "item" );
        var craftingItem = GsonHelper.getAsItem( object, "craftingItem", toolItem );
        return factory.create( id, adjective, craftingItem, new ItemStack( toolItem ) );
    }

    @Nonnull
    @Override
    public T fromNetwork( @Nonnull ResourceLocation id, @Nonnull FriendlyByteBuf buffer )
    {
        var adjective = buffer.readUtf();
        var craftingItem = buffer.readRegistryIdUnsafe( ForgeRegistries.ITEMS );
        var toolItem = buffer.readItem();
        return factory.create( id, adjective, craftingItem, toolItem );
    }

    @Override
    public void toNetwork( @Nonnull FriendlyByteBuf buffer, @Nonnull T upgrade )
    {
        buffer.writeUtf( upgrade.getUnlocalisedAdjective() );
        buffer.writeRegistryIdUnsafe( ForgeRegistries.ITEMS, upgrade.getCraftingItem().getItem() );
        buffer.writeItem( upgrade.item );
    }

    public interface Factory<T extends TurtleTool>
    {
        T create( ResourceLocation id, String adjective, Item craftItem, ItemStack toolItem );
    }
}
