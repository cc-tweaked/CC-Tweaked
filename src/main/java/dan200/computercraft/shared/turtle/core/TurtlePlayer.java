/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.util.FakeNetHandler;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
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

    public static final EntityType<TurtlePlayer> TYPE = EntityType.Builder.<TurtlePlayer>create( EntityClassification.MISC )
        .disableSerialization()
        .disableSummoning()
        .size( 0, 0 )
        .build( ComputerCraft.MOD_ID + ":turtle_player" );

    private TurtlePlayer( ITurtleAccess turtle )
    {
        super( (ServerWorld) turtle.getWorld(), getProfile( turtle.getOwningPlayer() ) );
        this.connection = new FakeNetHandler( this );
        setState( turtle );
    }

    private static GameProfile getProfile( @Nullable GameProfile profile )
    {
        return profile != null && profile.isComplete() ? profile : DEFAULT_PROFILE;
    }

    private void setState( ITurtleAccess turtle )
    {
        if( openContainer != null )
        {
            ComputerCraft.log.warn( "Turtle has open container ({})", openContainer );
            openContainer.onContainerClosed( this );
            openContainer = null;
        }

        BlockPos position = turtle.getPosition();
        posX = position.getX() + 0.5;
        posY = position.getY() + 0.5;
        posZ = position.getZ() + 0.5;

        rotationYaw = turtle.getDirection().getHorizontalAngle();
        rotationPitch = 0.0f;

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
        inventory.currentItem = 0;
        inventory.setInventorySlotContents( 0, currentStack );
    }

    public ItemStack unloadInventory( ITurtleAccess turtle )
    {
        // Get the item we placed with
        ItemStack results = inventory.getStackInSlot( 0 );
        inventory.setInventorySlotContents( 0, ItemStack.EMPTY );

        // Store (or drop) anything else we found
        BlockPos dropPosition = turtle.getPosition();
        Direction dropDirection = turtle.getDirection().getOpposite();
        for( int i = 0; i < inventory.getSizeInventory(); i++ )
        {
            ItemStack stack = inventory.getStackInSlot( i );
            if( !stack.isEmpty() )
            {
                ItemStack remainder = InventoryUtil.storeItems( stack, turtle.getItemHandler(), turtle.getSelectedSlot() );
                if( !remainder.isEmpty() )
                {
                    WorldUtil.dropItemStack( remainder, turtle.getWorld(), dropPosition, dropDirection );
                }
                inventory.setInventorySlotContents( i, ItemStack.EMPTY );
            }
        }
        inventory.markDirty();
        return results;
    }

    @Nonnull
    @Override
    public EntityType<?> getType()
    {
        return TYPE;
    }

    @Override
    public Vec3d getPositionVector()
    {
        return new Vec3d( posX, posY, posZ );
    }


    @Override
    public float getEyeHeight( @Nonnull Pose pose )
    {
        return 0;
    }

    @Override
    public float getStandingEyeHeight( Pose pose, EntitySize size )
    {
        return 0;
    }

    //region Code which depends on the connection
    @Nonnull
    @Override
    public OptionalInt openContainer( @Nullable INamedContainerProvider prover )
    {
        return OptionalInt.empty();
    }

    @Override
    public void sendEnterCombat()
    {
    }

    @Override
    public void sendEndCombat()
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
    public void openSignEditor( SignTileEntity signTile )
    {
    }

    @Override
    public void openHorseInventory( AbstractHorseEntity horse, IInventory inventory )
    {
    }

    @Override
    public void openBook( ItemStack stack, @Nonnull Hand hand )
    {
    }

    @Override
    public void closeScreen()
    {
    }

    @Override
    public void updateHeldItem()
    {
    }

    @Override
    protected void onNewPotionEffect( EffectInstance id )
    {
    }

    @Override
    protected void onChangedPotionEffect( EffectInstance id, boolean apply )
    {
    }

    @Override
    protected void onFinishedPotionEffect( EffectInstance effect )
    {
    }
    //endregion
}
