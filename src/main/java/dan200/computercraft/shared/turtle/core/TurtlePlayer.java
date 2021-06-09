/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.OptionalInt;
import java.util.UUID;

@SuppressWarnings( "EntityConstructor" )
public final class TurtlePlayer extends FakePlayer
{
    private static final GameProfile DEFAULT_PROFILE = new GameProfile( UUID.fromString( "0d0c4ca0-4ff1-11e4-916c-0800200c9a66" ), "[ComputerCraft]" );

    // TODO [M3R1-01] Fix Turtle not giving player achievement for actions
    private TurtlePlayer( ServerWorld world, GameProfile name )
    {
        super( world, name );
    }

    private static TurtlePlayer create( ITurtleAccess turtle )
    {
        ServerWorld world = (ServerWorld) turtle.getWorld();
        GameProfile profile = turtle.getOwningPlayer();

        TurtlePlayer player = new TurtlePlayer( world, getProfile( profile ) );
        player.networkHandler = new FakeNetHandler( player );
        player.setState( turtle );

        if( profile != null && profile.getId() != null )
        {
            // Constructing a player overrides the "active player" variable in advancements. As fake players cannot
            // get advancements, this prevents a normal player who has placed a turtle from getting advancements.
            // We try to locate the "actual" player and restore them.
            ServerPlayerEntity actualPlayer = world.getServer().getPlayerManager().getPlayer( player.getUuid() );
            if( actualPlayer != null ) player.getAdvancementTracker().setOwner( actualPlayer );
        }

        return player;
    }

    private static GameProfile getProfile( @Nullable GameProfile profile )
    {
        return profile != null && profile.isComplete() ? profile : DEFAULT_PROFILE;
    }

    private void setState( ITurtleAccess turtle )
    {
        if( this.currentScreenHandler != playerScreenHandler )
        {
            ComputerCraft.log.warn( "Turtle has open container ({})", this.currentScreenHandler );
            closeHandledScreen();
        }

        BlockPos position = turtle.getPosition();
        this.setPos( position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5 );

        this.yaw = turtle.getDirection()
            .asRotation();
        this.pitch = 0.0f;

        this.inventory.clear();
    }

    public static TurtlePlayer get( ITurtleAccess access )
    {
        if( !(access instanceof TurtleBrain) ) return create( access );

        TurtleBrain brain = (TurtleBrain) access;
        TurtlePlayer player = brain.cachedPlayer;
        if( player == null || player.getGameProfile() != getProfile( access.getOwningPlayer() ) || player.getEntityWorld() != access.getWorld() )
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
        this.inventory.selectedSlot = 0;
        this.inventory.setStack( 0, currentStack );
    }

    public ItemStack unloadInventory( ITurtleAccess turtle )
    {
        // Get the item we placed with
        ItemStack results = this.inventory.getStack( 0 );
        this.inventory.setStack( 0, ItemStack.EMPTY );

        // Store (or drop) anything else we found
        BlockPos dropPosition = turtle.getPosition();
        Direction dropDirection = turtle.getDirection()
            .getOpposite();
        for( int i = 0; i < this.inventory.size(); i++ )
        {
            ItemStack stack = this.inventory.getStack( i );
            if( !stack.isEmpty() )
            {
                ItemStack remainder = InventoryUtil.storeItems( stack, turtle.getItemHandler(), turtle.getSelectedSlot() );
                if( !remainder.isEmpty() )
                {
                    WorldUtil.dropItemStack( remainder, turtle.getWorld(), dropPosition, dropDirection );
                }
                this.inventory.setStack( i, ItemStack.EMPTY );
            }
        }
        this.inventory.markDirty();
        return results;
    }

    @Nonnull
    @Override
    public EntityType<?> getType()
    {
        return ComputerCraftRegistry.ModEntities.TURTLE_PLAYER;
    }

    @Override
    public float getEyeHeight( @Nonnull EntityPose pose )
    {
        return 0;
    }

    @Override
    public Vec3d getPos()
    {
        return new Vec3d( this.getX(), this.getY(), this.getZ() );
    }

    @Override
    public float getActiveEyeHeight( @Nonnull EntityPose pose, @Nonnull EntityDimensions size )
    {
        return 0;
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

    //region Code which depends on the connection
    @Nonnull
    @Override
    public OptionalInt openHandledScreen( @Nullable NamedScreenHandlerFactory prover )
    {
        return OptionalInt.empty();
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
