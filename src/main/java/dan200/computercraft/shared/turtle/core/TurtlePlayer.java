/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.FakePlayer;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.util.FakeNetHandler;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.OptionalInt;
import java.util.UUID;

@SuppressWarnings( "EntityConstructor" )
public final class TurtlePlayer extends FakePlayer
{
    private static final GameProfile DEFAULT_PROFILE = new GameProfile( UUID.fromString( "0d0c4ca0-4ff1-11e4-916c-0800200c9a66" ), "[ComputerCraft]" );

    // TODO [M3R1-01] Fix Turtle not giving player achievement for actions
    private TurtlePlayer( ServerLevel world, GameProfile name )
    {
        super( world, name );
    }

    private static TurtlePlayer create( ITurtleAccess turtle )
    {
        ServerLevel world = (ServerLevel) turtle.getLevel();
        GameProfile profile = turtle.getOwningPlayer();

        TurtlePlayer player = new TurtlePlayer( world, getProfile( profile ) );
        player.connection = new FakeNetHandler( player );
        player.setState( turtle );

        if( profile != null && profile.getId() != null )
        {
            // Constructing a player overrides the "active player" variable in advancements. As fake players cannot
            // get advancements, this prevents a normal player who has placed a turtle from getting advancements.
            // We try to locate the "actual" player and restore them.
            ServerPlayer actualPlayer = world.getServer().getPlayerList().getPlayer( player.getUUID() );
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
            closeContainer();
        }

        BlockPos position = turtle.getPosition();
        setPosRaw( position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5 );

        setYRot( turtle.getDirection()
            .toYRot() );
        setXRot( 0.0f );

        getInventory().clearContent();
    }

    public static TurtlePlayer get( ITurtleAccess access )
    {
        if( !(access instanceof TurtleBrain) ) return create( access );

        TurtleBrain brain = (TurtleBrain) access;
        TurtlePlayer player = brain.cachedPlayer;
        if( player == null || player.getGameProfile() != getProfile( access.getOwningPlayer() ) || player.getCommandSenderWorld() != access.getLevel() )
        {
            player = brain.cachedPlayer = create( brain );
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
        getInventory().selected = 0;
        getInventory().setItem( 0, currentStack );
    }

    public ItemStack unloadInventory( ITurtleAccess turtle )
    {
        // Get the item we placed with
        ItemStack results = getInventory().getItem( 0 );
        getInventory().setItem( 0, ItemStack.EMPTY );

        // Store (or drop) anything else we found
        BlockPos dropPosition = turtle.getPosition();
        Direction dropDirection = turtle.getDirection()
            .getOpposite();
        for( int i = 0; i < getInventory().getContainerSize(); i++ )
        {
            ItemStack stack = getInventory().getItem( i );
            if( !stack.isEmpty() )
            {
                ItemStack remainder = InventoryUtil.storeItems( stack, turtle.getItemHandler(), turtle.getSelectedSlot() );
                if( !remainder.isEmpty() )
                {
                    WorldUtil.dropItemStack( remainder, turtle.getLevel(), dropPosition, dropDirection );
                }
                getInventory().setItem( i, ItemStack.EMPTY );
            }
        }
        getInventory().setChanged();
        return results;
    }

    @Nonnull
    @Override
    public EntityType<?> getType()
    {
        return ComputerCraftRegistry.ModEntities.TURTLE_PLAYER;
    }

    @Override
    public float getEyeHeight( @Nonnull Pose pose )
    {
        return 0;
    }

    @Override
    public Vec3 position()
    {
        return new Vec3( getX(), getY(), getZ() );
    }

    @Override
    public float getStandingEyeHeight( @Nonnull Pose pose, @Nonnull EntityDimensions size )
    {
        return 0;
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
    public void openTextEdit( @Nonnull SignBlockEntity signTile )
    {
    }

    //region Code which depends on the connection
    @Nonnull
    @Override
    public OptionalInt openMenu( @Nullable MenuProvider prover )
    {
        return OptionalInt.empty();
    }

    @Override
    public void openHorseInventory( @Nonnull AbstractHorse horse, @Nonnull Container inventory )
    {
    }

    @Override
    public void closeContainer()
    {
    }

    @Override
    protected void onEffectRemoved( @Nonnull MobEffectInstance effect )
    {
    }
    //endregion
}
