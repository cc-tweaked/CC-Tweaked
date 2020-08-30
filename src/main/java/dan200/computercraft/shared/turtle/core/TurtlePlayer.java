/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import java.util.OptionalInt;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.FakePlayer;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.util.FakeNetHandler;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

@SuppressWarnings ("EntityConstructor")
public final class TurtlePlayer extends FakePlayer
{
    private static final GameProfile DEFAULT_PROFILE = new GameProfile(
        UUID.fromString( "0d0c4ca0-4ff1-11e4-916c-0800200c9a66" ),
        "[ComputerCraft]"
    );

    private TurtlePlayer( ITurtleAccess turtle )
    {
        super( (ServerWorld) turtle.getWorld(), getProfile( turtle.getOwningPlayer() ) );
        this.networkHandler = new FakeNetHandler( this );
        setState( turtle );
    }

    private static GameProfile getProfile( @Nullable GameProfile profile )
    {
        return profile != null && profile.isComplete() ? profile : DEFAULT_PROFILE;
    }

    private void setState( ITurtleAccess turtle )
    {
        if( currentScreenHandler != null )
        {
            ComputerCraft.log.warn( "Turtle has open container ({})", currentScreenHandler );
            currentScreenHandler.close( this );
            currentScreenHandler = null;
        }

        BlockPos position = turtle.getPosition();
        setPos( position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5 );

        yaw = turtle.getDirection().asRotation();
        pitch = 0.0f;

        inventory.clear();
    }

    public static TurtlePlayer get( ITurtleAccess access )
    {
        if( !(access instanceof TurtleBrain) ) return new TurtlePlayer( access );

        TurtleBrain brain = (TurtleBrain) access;
        TurtlePlayer player = brain.m_cachedPlayer;
        if( player == null || player.getGameProfile() != getProfile( access.getOwningPlayer() )
            || player.getEntityWorld() != access.getWorld() )
        {
            player = brain.m_cachedPlayer = new TurtlePlayer( brain );
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
        inventory.selectedSlot = 0;
        inventory.setStack( 0, currentStack );
    }

    public ItemStack unloadInventory( ITurtleAccess turtle )
    {
        // Get the item we placed with
        ItemStack results = inventory.getStack( 0 );
        inventory.setStack( 0, ItemStack.EMPTY );

        // Store (or drop) anything else we found
        BlockPos dropPosition = turtle.getPosition();
        Direction dropDirection = turtle.getDirection().getOpposite();
        for( int i = 0; i < inventory.size(); i++ )
        {
            ItemStack stack = inventory.getStack( i );
            if( !stack.isEmpty() )
            {
                ItemStack remainder = InventoryUtil.storeItems( stack, turtle.getItemHandler(), turtle.getSelectedSlot() );
                if( !remainder.isEmpty() )
                {
                    WorldUtil.dropItemStack( remainder, turtle.getWorld(), dropPosition, dropDirection );
                }
                inventory.setStack( i, ItemStack.EMPTY );
            }
        }
        inventory.markDirty();
        return results;
    }

    @Nonnull
    @Override
    public EntityType<?> getType()
    {
        return Registry.ModEntities.TURTLE_PLAYER;
    }

    @Override
    public Vec3d getPos()
    {
        return new Vec3d( getX(), getY(), getZ() );
    }

    @Override
    public float getEyeHeight( @Nonnull EntityPose pose )
    {
        return 0;
    }

    @Override
    public float getActiveEyeHeight( @Nonnull EntityPose pose, @Nonnull EntityDimensions size )
    {
        return 0;
    }

    //region Code which depends on the connection
    @Nonnull
    @Override
    public OptionalInt openHandledScreen( @Nullable NamedScreenHandlerFactory prover )
    {
        return OptionalInt.empty();
    }

    @Override
    public void enterCombat()
    {
    }

    @Override
    public void endCombat()
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
    public void openEditSignScreen( @Nonnull SignBlockEntity signTile )
    {
    }

    @Override
    public void openHorseInventory( @Nonnull HorseBaseEntity horse, @Nonnull Inventory inventory )
    {
    }

    @Override
    public void openEditBookScreen( @Nonnull ItemStack stack, @Nonnull Hand hand )
    {
    }

    @Override
    public void closeHandledScreen()
    {
    }

    @Override
    public void updateCursorStack()
    {
    }

    @Override
    protected void onStatusEffectApplied( @Nonnull StatusEffectInstance id )
    {
    }

    @Override
    protected void onStatusEffectUpgraded( @Nonnull StatusEffectInstance id, boolean apply )
    {
    }

    @Override
    protected void onStatusEffectRemoved( @Nonnull StatusEffectInstance effect )
    {
    }
    //endregion
}
