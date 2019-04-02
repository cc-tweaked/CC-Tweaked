/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.network.container;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IInteractionObject;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A horrible hack to allow opening GUIs until Forge adds a built-in system.
 */
public interface ContainerType<T extends Container> extends IInteractionObject
{
    @Nonnull
    ResourceLocation getId();

    void toBytes( PacketBuffer buf );

    void fromBytes( PacketBuffer buf );

    @Nonnull
    @Override
    @SuppressWarnings( "unchecked" )
    default T createContainer( @Nonnull InventoryPlayer inventoryPlayer, @Nonnull EntityPlayer entityPlayer )
    {
        return ((BiFunction<ContainerType<T>, EntityPlayer, T>) containerFactories.get( getId() )).apply( this, entityPlayer );
    }

    @Nonnull
    @Override
    default String getGuiID()
    {
        return getId().toString();
    }

    @Nonnull
    @Override
    default ITextComponent getName()
    {
        return new TextComponentString( "" );
    }

    @Override
    default boolean hasCustomName()
    {
        return false;
    }

    @Nullable
    @Override
    default ITextComponent getCustomName()
    {
        return null;
    }

    default void open( EntityPlayer player )
    {
        NetworkHooks.openGui( (EntityPlayerMP) player, this, this::toBytes );
    }

    static <C extends Container, T extends ContainerType<C>> void register( Supplier<T> containerType, BiFunction<T, EntityPlayer, C> factory )
    {
        factories.put( containerType.get().getId(), containerType );
        containerFactories.put( containerType.get().getId(), factory );
    }

    static <C extends Container, T extends ContainerType<C>> void registerGui( Supplier<T> containerType, BiFunction<T, EntityPlayer, GuiContainer> factory )
    {
        guiFactories.put( containerType.get().getId(), factory );
    }

    static <C extends Container, T extends ContainerType<C>> void registerGui( Supplier<T> containerType, Function<C, GuiContainer> factory )
    {
        registerGui( containerType, ( type, player ) -> {
            @SuppressWarnings( "unchecked" )
            C container = ((BiFunction<T, EntityPlayer, C>) containerFactories.get( type.getId() )).apply( type, player );
            return container == null ? null : factory.apply( container );
        } );
    }

    Map<ResourceLocation, Supplier<? extends ContainerType<?>>> factories = new HashMap<>();
    Map<ResourceLocation, BiFunction<? extends ContainerType<?>, EntityPlayer, GuiContainer>> guiFactories = new HashMap<>();
    Map<ResourceLocation, BiFunction<? extends ContainerType<?>, EntityPlayer, ? extends Container>> containerFactories = new HashMap<>();
}
