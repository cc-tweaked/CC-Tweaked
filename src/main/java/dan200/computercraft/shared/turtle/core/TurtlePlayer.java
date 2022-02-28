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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
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

    private TurtlePlayer( ServerLevel world, GameProfile name )
    {
        super( world, name );
    }

    private static TurtlePlayer create( ITurtleAccess turtle )
    {
        ServerLevel world = (ServerLevel) turtle.getLevel();
        GameProfile profile = turtle.getOwningPlayer();

        TurtlePlayer player = new TurtlePlayer( world, getProfile( profile ) );
        player.setState( turtle );

        if( profile != null && profile.getId() != null )
        {
            // Constructing a player overrides the "active player" variable in advancements. As fake players cannot
            // get advancements, this prevents a normal player who has placed a turtle from getting advancements.
            // We try to locate the "actual" player and restore them.
            ServerPlayer actualPlayer = world.getServer().getPlayerList().getPlayer( profile.getId() );
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
        if( !(access instanceof TurtleBrain brain) ) return create( access );

        TurtlePlayer player = brain.cachedPlayer;
        if( player == null || player.getGameProfile() != getProfile( access.getOwningPlayer() )
            || player.getCommandSenderWorld() != access.getLevel() )
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

        setRot( turtle.getDirection().toYRot(), 0 );

        getInventory().clearContent();
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
            setRot( direction.toYRot(), 0 );
        }
        else
        {
            setRot( turtle.getDirection().toYRot(), DirectionUtil.toPitchAngle( direction ) );
        }

        setPosRaw( posX, posY, posZ );
        xo = posX;
        yo = posY;
        zo = posZ;
        xRotO = getXRot();
        yRotO = getYRot();

        yHeadRot = getYRot();
        yHeadRotO = yHeadRot;
    }

    public void loadInventory( @Nonnull ItemStack stack )
    {
        getInventory().clearContent();
        getInventory().selected = 0;
        getInventory().setItem( 0, stack );
    }

    public void loadInventory( @Nonnull ITurtleAccess turtle )
    {
        getInventory().clearContent();

        int currentSlot = turtle.getSelectedSlot();
        int slots = turtle.getItemHandler().getSlots();

        // Load up the fake inventory
        getInventory().selected = 0;
        for( int i = 0; i < slots; i++ )
        {
            getInventory().setItem( i, turtle.getItemHandler().getStackInSlot( (currentSlot + i) % slots ) );
        }
    }

    public void unloadInventory( ITurtleAccess turtle )
    {
        int currentSlot = turtle.getSelectedSlot();
        int slots = turtle.getItemHandler().getSlots();

        // Load up the fake inventory
        getInventory().selected = 0;
        for( int i = 0; i < slots; i++ )
        {
            turtle.getItemHandler().setStackInSlot( (currentSlot + i) % slots, getInventory().getItem( i ) );
        }

        // Store (or drop) anything else we found
        BlockPos dropPosition = turtle.getPosition();
        Direction dropDirection = turtle.getDirection().getOpposite();
        int totalSize = getInventory().getContainerSize();
        for( int i = slots; i < totalSize; i++ )
        {
            ItemStack remainder = InventoryUtil.storeItems( getInventory().getItem( i ), turtle.getItemHandler(), turtle.getSelectedSlot() );
            if( !remainder.isEmpty() )
            {
                WorldUtil.dropItemStack( remainder, turtle.getLevel(), dropPosition, dropDirection );
            }
        }

        getInventory().setChanged();
    }

    @Override
    public Vec3 position()
    {
        return new Vec3( getX(), getY(), getZ() );
    }

    @Override
    public float getEyeHeight( @Nonnull Pose pose )
    {
        return 0;
    }

    @Override
    public float getStandingEyeHeight( @Nonnull Pose pose, @Nonnull EntityDimensions size )
    {
        return 0;
    }

    //region Code which depends on the connection
    @Nonnull
    @Override
    public OptionalInt openMenu( @Nullable MenuProvider prover )
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
    public void openTextEdit( @Nonnull SignBlockEntity signTile )
    {
    }

    @Override
    public void openHorseInventory( @Nonnull AbstractHorse horse, @Nonnull Container inventory )
    {
    }

    @Override
    public void openItemGui( @Nonnull ItemStack stack, @Nonnull InteractionHand hand )
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
