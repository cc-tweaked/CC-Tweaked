/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.turtle;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.shared.util.FakeNetHandler;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * A wrapper for {@link ServerPlayer} which denotes a "fake" player.
 *
 * Please note that this does not implement any of the traditional fake player behaviour. It simply exists to prevent me passing in normal players.
 */
public abstract class FakePlayer extends ServerPlayer
{
    public FakePlayer( ServerLevel world, GameProfile gameProfile )
    {
        super( world.getServer(), world, gameProfile );
        connection = new FakeNetHandler( this );
    }

    // region Direct networkHandler access
    @Override
    public void onEnterCombat()
    {
    }

    @Override
    public void onLeaveCombat()
    {
    }

    @Override
    public void tick()
    {
    }

    @Override
    public void doTick()
    {
    }

    @Override
    public void die( DamageSource damage )
    {
    }

    @Override
    public Entity changeDimension( ServerLevel destination )
    {
        return this;
    }

    @Override
    public void stopSleepInBed( boolean bl, boolean updateSleepingPlayers )
    {

    }

    @Override
    public boolean startRiding( Entity entity, boolean flag )
    {
        return false;
    }

    @Override
    public void stopRiding()
    {
    }

    @Override
    public void openTextEdit( SignBlockEntity tile )
    {
    }

    @Override
    public OptionalInt openMenu( @Nullable MenuProvider container )
    {
        return OptionalInt.empty();
    }

    @Override
    public void sendMerchantOffers( int id, MerchantOffers list, int level, int experience, boolean levelled, boolean refreshable )
    {
    }

    @Override
    public void openHorseInventory( AbstractHorse horse, Container inventory )
    {
    }

    @Override
    public void openItemGui( ItemStack stack, InteractionHand hand )
    {
    }

    @Override
    public void openCommandBlock( CommandBlockEntity block )
    {
    }

    //    @Override
    //    public void onSlotUpdate( ScreenHandler container, int slot, ItemStack stack )
    //    {
    //    }
    //
    //    @Override
    //    public void onHandlerRegistered( ScreenHandler container, DefaultedList<ItemStack> defaultedList )
    //    {
    //    }
    //
    //    @Override
    //    public void onPropertyUpdate( ScreenHandler container, int key, int value )
    //    {
    //    }

    @Override
    public void closeContainer()
    {
    }

    //    @Override
    //    public void updateCursorStack()
    //    {
    //    }

    @Override
    public int awardRecipes( Collection<Recipe<?>> recipes )
    {
        return 0;
    }

    // Indirect
    @Override
    public int resetRecipes( Collection<Recipe<?>> recipes )
    {
        return 0;
    }

    @Override
    public void displayClientMessage( Component textComponent, boolean status )
    {
    }

    @Override
    protected void completeUsingItem()
    {
    }

    @Override
    public void lookAt( EntityAnchorArgument.Anchor anchor, Vec3 vec3d )
    {
    }

    @Override
    public void lookAt( EntityAnchorArgument.Anchor self, Entity entity, EntityAnchorArgument.Anchor target )
    {
    }

    @Override
    protected void onEffectAdded( MobEffectInstance statusEffectInstance, @Nullable Entity source )
    {
    }

    @Override
    protected void onEffectUpdated( MobEffectInstance statusEffectInstance, boolean particles, @Nullable Entity source )
    {
    }

    @Override
    protected void onEffectRemoved( MobEffectInstance statusEffectInstance )
    {
    }

    @Override
    public void teleportTo( double x, double y, double z )
    {
    }

    //    @Override
    //    public void setGameMode( GameMode gameMode )
    //    {
    //    }

    @Override
    public void sendMessage( Component message, ChatType type, UUID senderUuid )
    {

    }

    @Override
    public String getIpAddress()
    {
        return "[Fake Player]";
    }

    //    @Override
    //    public void sendResourcePackUrl( String url, String hash )
    //    {
    //    }

    //    @Override
    //    public void onStoppedTracking( Entity entity )
    //    {
    //    }

    @Override
    public void setCamera( Entity entity )
    {
    }

    @Override
    public void teleportTo( ServerLevel serverWorld, double x, double y, double z, float pitch, float yaw )
    {
    }

    @Override
    public void trackChunk( ChunkPos chunkPos, Packet<?> packet )
    {
    }

    @Override
    public void untrackChunk( ChunkPos chunkPos )
    {
    }

    @Override
    public void playNotifySound( SoundEvent soundEvent, SoundSource soundCategory, float volume, float pitch )
    {
    }
}
