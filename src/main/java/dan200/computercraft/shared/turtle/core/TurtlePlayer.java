/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.util.FakeNetHandler;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.OptionalInt;
import java.util.UUID;

public final class TurtlePlayer extends FakePlayer
{
    private static final GameProfile DEFAULT_PROFILE = new GameProfile(
        UUID.fromString( "0d0c4ca0-4ff1-11e4-916c-0800200c9a66" ),
        "[ComputerCraft]"
    );

    private TurtlePlayer( ServerWorld world, GameProfile name )
    {
        super( world, name );
    }

    private static TurtlePlayer create( ITurtleAccess turtle )
    {
        ServerWorld world = (ServerWorld) turtle.getWorld();
        GameProfile profile = turtle.getOwningPlayer();

        TurtlePlayer player = new TurtlePlayer( world, getProfile( profile ) );
        player.connection = new FakeNetHandler( player );
        player.setState( turtle );

        if( profile != null && profile.getId() != null )
        {
            // Constructing a player overrides the "active player" variable in advancements. As fake players cannot
            // get advancements, this prevents a normal player who has placed a turtle from getting advancements.
            // We try to locate the "actual" player and restore them.
            ServerPlayerEntity actualPlayer = world.getServer().getPlayerList().getPlayer( profile.getId() );
            if( actualPlayer != null ) player.getAdvancements().setPlayer( actualPlayer );
        }

        return player;
    }

    private static GameProfile getProfile( @Nullable GameProfile profile )
    {
        return profile != null && profile.isComplete() ? profile : DEFAULT_PROFILE;
    }

    private void setState( ITurtleAccess turtle )
    {
        if( containerMenu != inventoryMenu )
        {
            ComputerCraft.log.warn( "Turtle has open container ({})", containerMenu );
            doCloseContainer();
        }

        BlockPos position = turtle.getPosition();
        setPosRaw( position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5 );

        yRot = turtle.getDirection().toYRot();
        xRot = 0.0f;

        inventory.clearContent();
    }

    public static TurtlePlayer get( ITurtleAccess access )
    {
        if( !(access instanceof TurtleBrain) ) return create( access );

        TurtleBrain brain = (TurtleBrain) access;
        TurtlePlayer player = brain.m_cachedPlayer;
        if( player == null || player.getGameProfile() != getProfile( access.getOwningPlayer() )
            || player.getCommandSenderWorld() != access.getWorld() )
        {
            player = brain.m_cachedPlayer = create( brain );
        }
        else
        {
            player.setState( access );
        }

        return player;
    }

    public void loadInventory( @Nonnull ItemStack currentStack )
    {
        // Load up the fake inventory
        inventory.selected = 0;
        inventory.setItem( 0, currentStack );
    }

    public ItemStack unloadInventory( ITurtleAccess turtle )
    {
        // Get the item we placed with
        ItemStack results = inventory.getItem( 0 );
        inventory.setItem( 0, ItemStack.EMPTY );

        // Store (or drop) anything else we found
        BlockPos dropPosition = turtle.getPosition();
        Direction dropDirection = turtle.getDirection().getOpposite();
        for( int i = 0; i < inventory.getContainerSize(); i++ )
        {
            ItemStack stack = inventory.getItem( i );
            if( !stack.isEmpty() )
            {
                ItemStack remainder = InventoryUtil.storeItems( stack, turtle.getItemHandler(), turtle.getSelectedSlot() );
                if( !remainder.isEmpty() )
                {
                    WorldUtil.dropItemStack( remainder, turtle.getWorld(), dropPosition, dropDirection );
                }
                inventory.setItem( i, ItemStack.EMPTY );
            }
        }
        inventory.setChanged();
        return results;
    }

    @Nonnull
    @Override
    public EntityType<?> getType()
    {
        return Registry.ModEntities.TURTLE_PLAYER.get();
    }

    @Override
    public Vec3d getCommandSenderWorldPosition()
    {
        return position();
    }

    @Override
    public float getEyeHeight( @Nonnull Pose pose )
    {
        return 0;
    }

    @Override
    public float getStandingEyeHeight( @Nonnull Pose pose, @Nonnull EntitySize size )
    {
        return 0;
    }

    //region Code which depends on the connection
    @Nonnull
    @Override
    public OptionalInt openMenu( @Nullable INamedContainerProvider prover )
    {
        return OptionalInt.empty();
    }

    @Override
    public void onEnterCombat()
    {
    }

    @Override
    public void onLeaveCombat()
    {
    }

    @Override
    public boolean startRiding( @Nonnull Entity entityIn, boolean force )
    {
        return false;
    }

    @Override
    public void stopRiding()
    {
    }

    @Override
    public void openTextEdit( @Nonnull SignTileEntity signTile )
    {
    }

    @Override
    public void openHorseInventory( @Nonnull AbstractHorseEntity horse, @Nonnull IInventory inventory )
    {
    }

    @Override
    public void openItemGui( @Nonnull ItemStack stack, @Nonnull Hand hand )
    {
    }

    @Override
    public void closeContainer()
    {
    }

    @Override
    public void broadcastCarriedItem()
    {
    }

    @Override
    protected void onEffectAdded( @Nonnull EffectInstance id )
    {
    }

    @Override
    protected void onEffectUpdated( @Nonnull EffectInstance id, boolean apply )
    {
    }

    @Override
    protected void onEffectRemoved( @Nonnull EffectInstance effect )
    {
    }
    //endregion
}
