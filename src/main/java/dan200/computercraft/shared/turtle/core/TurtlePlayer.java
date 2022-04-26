/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
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
import net.minecraft.util.math.vector.Vector3d;
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

    public static TurtlePlayer get( ITurtleAccess access )
    {
        if( !(access instanceof TurtleBrain) ) return create( access );

        TurtleBrain brain = (TurtleBrain) access;
        TurtlePlayer player = brain.cachedPlayer;
        if( player == null || player.getGameProfile() != getProfile( access.getOwningPlayer() )
            || player.getCommandSenderWorld() != access.getWorld() )
        {
            player = brain.cachedPlayer = create( brain );
        }
        else
        {
            player.setState( access );
        }

        return player;
    }

    public static TurtlePlayer getWithPosition( ITurtleAccess turtle, BlockPos position, Direction direction )
    {
        TurtlePlayer turtlePlayer = get( turtle );
        turtlePlayer.setPosition( turtle, position, direction );
        return turtlePlayer;
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

    public void setPosition( ITurtleAccess turtle, BlockPos position, Direction direction )
    {
        double posX = position.getX() + 0.5;
        double posY = position.getY() + 0.5;
        double posZ = position.getZ() + 0.5;

        // Stop intersection with the turtle itself
        if( turtle.getPosition().equals( position ) )
        {
            posX += 0.48 * direction.getStepX();
            posY += 0.48 * direction.getStepY();
            posZ += 0.48 * direction.getStepZ();
        }

        if( direction.getAxis() != Direction.Axis.Y )
        {
            yRot = direction.toYRot();
            xRot = 0.0f;
        }
        else
        {
            yRot = turtle.getDirection().toYRot();
            xRot = DirectionUtil.toPitchAngle( direction );
        }

        setPosRaw( posX, posY, posZ );
        xo = posX;
        yo = posY;
        zo = posZ;
        xRotO = xRot;
        yRotO = yRot;

        yHeadRot = yRot;
        yHeadRotO = yHeadRot;
    }

    public void loadInventory( @Nonnull ItemStack stack )
    {
        inventory.clearContent();
        inventory.selected = 0;
        inventory.setItem( 0, stack );
    }

    public void loadInventory( @Nonnull ITurtleAccess turtle )
    {
        inventory.clearContent();

        int currentSlot = turtle.getSelectedSlot();
        int slots = turtle.getItemHandler().getSlots();

        // Load up the fake inventory
        inventory.selected = 0;
        for( int i = 0; i < slots; i++ )
        {
            inventory.setItem( i, turtle.getItemHandler().getStackInSlot( (currentSlot + i) % slots ) );
        }
    }

    public void unloadInventory( ITurtleAccess turtle )
    {
        int currentSlot = turtle.getSelectedSlot();
        int slots = turtle.getItemHandler().getSlots();

        // Load up the fake inventory
        inventory.selected = 0;
        for( int i = 0; i < slots; i++ )
        {
            turtle.getItemHandler().setStackInSlot( (currentSlot + i) % slots, inventory.getItem( i ) );
        }

        // Store (or drop) anything else we found
        BlockPos dropPosition = turtle.getPosition();
        Direction dropDirection = turtle.getDirection().getOpposite();
        int totalSize = inventory.getContainerSize();
        for( int i = slots; i < totalSize; i++ )
        {
            ItemStack remainder = InventoryUtil.storeItems( inventory.getItem( i ), turtle.getItemHandler(), turtle.getSelectedSlot() );
            if( !remainder.isEmpty() )
            {
                WorldUtil.dropItemStack( remainder, turtle.getWorld(), dropPosition, dropDirection );
            }
        }

        inventory.setChanged();
    }

    @Override
    public Vector3d position()
    {
        return new Vector3d( getX(), getY(), getZ() );
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
