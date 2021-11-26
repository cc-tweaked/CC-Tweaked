/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.api.upgrades.IUpgradeBase;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.SerializationTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;

public final class TurtleToolSerialiser extends TurtleUpgradeSerialiser.Base<TurtleTool>
{
    public static final TurtleToolSerialiser INSTANCE = new TurtleToolSerialiser();

    private TurtleToolSerialiser() {}

    @Nonnull
    @Override
    public TurtleTool fromJson( @Nonnull ResourceLocation id, @Nonnull JsonObject object )
    {
        var adjective = GsonHelper.getAsString( object, "adjective", IUpgradeBase.getDefaultAdjective( id ) );
        var toolItem = GsonHelper.getAsItem( object, "item" );
        var craftingItem = GsonHelper.getAsItem( object, "craftingItem", toolItem );
        var damageMultiplier = GsonHelper.getAsFloat( object, "damageMultiplier", 3.0f );

        ResourceLocation breakableName = null;
        Tag<Block> breakable = null;
        if( object.has( "breakable" ) )
        {
            breakableName = new ResourceLocation( GsonHelper.getAsString( object, "breakable" ) );
            breakable = SerializationTags.getInstance().getTagOrThrow(
                Registry.BLOCK_REGISTRY, breakableName,
                tagId -> new JsonSyntaxException( "Unknown item tag '" + tagId + "'" )
            );
        }

        return new TurtleTool( id, adjective, craftingItem, new ItemStack( toolItem ), damageMultiplier, breakableName, breakable );
    }

    @Nonnull
    @Override
    public TurtleTool fromNetwork( @Nonnull ResourceLocation id, @Nonnull FriendlyByteBuf buffer )
    {
        var adjective = buffer.readUtf();
        var craftingItem = buffer.readRegistryIdUnsafe( ForgeRegistries.ITEMS );
        var toolItem = buffer.readItem();
        // damageMultiplier and breakable aren't used by the client, but we need to construct the upgrade exactly
        // as otherwise syncing on an SP world will overwrite the (shared) upgrade registry with an invalid upgrade!
        var damageMultiplier = buffer.readFloat();

        ResourceLocation breakableName = null;
        Tag<Block> breakable = null;
        if( buffer.readBoolean() )
        {
            breakableName = buffer.readResourceLocation();
            breakable = SerializationTags.getInstance().getOrEmpty( Registry.BLOCK_REGISTRY ).getTagOrEmpty( breakableName );
        }
        return new TurtleTool( id, adjective, craftingItem, toolItem, damageMultiplier, breakableName, breakable );
    }

    @Override
    public void toNetwork( @Nonnull FriendlyByteBuf buffer, @Nonnull TurtleTool upgrade )
    {
        buffer.writeUtf( upgrade.getUnlocalisedAdjective() );
        buffer.writeRegistryIdUnsafe( ForgeRegistries.ITEMS, upgrade.getCraftingItem().getItem() );
        buffer.writeItem( upgrade.item );
        buffer.writeFloat( upgrade.damageMulitiplier );
        buffer.writeBoolean( upgrade.breakableName != null );
        if( upgrade.breakableName != null ) buffer.writeResourceLocation( upgrade.breakableName );
    }
}
