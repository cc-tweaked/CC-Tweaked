/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.api.turtle.event;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.command.arguments.EntityAnchorArgumentType;
import net.minecraft.container.Container;
import net.minecraft.container.NameableContainerProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.MessageType;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.network.packet.RequestCommandCompletionsC2SPacket;
import net.minecraft.server.network.packet.VehicleMoveC2SPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.Hand;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TraderOfferList;
import net.minecraft.world.GameMode;
import net.minecraft.world.dimension.DimensionType;

import javax.annotation.Nullable;
import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.OptionalInt;

/**
 * A wrapper for {@link ServerPlayerEntity} which denotes a "fake" player.
 *
 * Please note that this does not implement any of the traditional fake player behaviour. It simply exists to prevent
 * me passing in normal players.
 */
public class FakePlayer extends ServerPlayerEntity
{
    public FakePlayer( ServerWorld world, GameProfile gameProfile )
    {
        super( world.getServer(), world, gameProfile, new ServerPlayerInteractionManager( world ) );
        networkHandler = new FakeNetHandler( this );
    }

    // region Direct networkHandler access
    @Override
    public void method_6000() { }

    @Override
    public void method_6044() { }

    @Override
    public void tick() { }

    @Override
    public void method_14226() { }

    @Override
    public void onDeath( DamageSource damage ) { }

    @Nullable
    @Override
    public Entity changeDimension( DimensionType dimension )
    {
        return this;
    }

    @Override
    public void wakeUp( boolean resetTimer, boolean notify, boolean setSpawn ) { }

    @Override
    public boolean startRiding( Entity entity, boolean flag )
    {
        return false;
    }

    @Override
    public void stopRiding() { }

    @Override
    public void openEditSignScreen( SignBlockEntity tile ) { }

    @Override
    public OptionalInt openContainer( @Nullable NameableContainerProvider container )
    {
        return OptionalInt.empty();
    }

    @Override
    public void sendTradeOffers( int id, TraderOfferList list, int level, int experience, boolean levelled, boolean refreshable ) { }

    @Override
    public void openHorseInventory( HorseBaseEntity horse, Inventory inventory ) { }

    @Override
    public void openEditBookScreen( ItemStack stack, Hand hand ) { }

    @Override
    public void openCommandBlockScreen( CommandBlockBlockEntity block ) { }

    @Override
    public void onContainerSlotUpdate( Container container, int slot, ItemStack stack ) { }

    @Override
    public void onContainerRegistered( Container container, DefaultedList<ItemStack> defaultedList ) { }

    @Override
    public void onContainerPropertyUpdate( Container container, int key, int value ) { }

    @Override
    public void closeContainer() { }

    @Override
    public void method_14241() { }

    @Override
    public void addChatMessage( Text textComponent, boolean status ) { }

    @Override
    protected void method_6040() { }

    @Override
    public void lookAt( EntityAnchorArgumentType.EntityAnchor anchor, Vec3d vec3d ) { }

    @Override
    public void method_14222( EntityAnchorArgumentType.EntityAnchor self, Entity entity, EntityAnchorArgumentType.EntityAnchor target ) { }

    @Override
    protected void method_6020( StatusEffectInstance statusEffectInstance ) { }

    @Override
    protected void method_6009( StatusEffectInstance statusEffectInstance, boolean particles ) { }

    @Override
    protected void method_6129( StatusEffectInstance statusEffectInstance ) { }

    @Override
    public void requestTeleport( double x, double y, double z ) { }

    @Override
    public void setGameMode( GameMode gameMode ) { }

    @Override
    public void sendChatMessage( Text textComponent, MessageType chatMessageType ) { }

    @Override
    public String getServerBrand()
    {
        return "[Fake Player]";
    }

    @Override
    public void method_14255( String url, String hash ) { }

    @Override
    public void onStoppedTracking( Entity entity ) { }

    @Override
    public void setCameraEntity( Entity entity ) { }

    @Override
    public void teleport( ServerWorld serverWorld, double x, double y, double z, float pitch, float yaw ) { }

    @Override
    public void sendInitialChunkPackets( ChunkPos chunkPos, Packet<?> packet, Packet<?> packet2 ) { }

    @Override
    public void sendUnloadChunkPacket( ChunkPos chunkPos ) { }

    @Override
    public void playSound( SoundEvent soundEvent, SoundCategory soundCategory, float volume, float pitch ) { }
    // endregion

    // Indirect
    @Override
    public int lockRecipes( Collection<Recipe<?>> recipes )
    {
        return 0;
    }

    @Override
    public int unlockRecipes( Collection<Recipe<?>> recipes )
    {
        return 0;
    }
    //

    private static class FakeNetHandler extends ServerPlayNetworkHandler
    {
        FakeNetHandler( ServerPlayerEntity player )
        {
            super( player.server, new FakeConnection(), player );
        }

        @Override
        public void disconnect( Text message ) { }

        @Override
        public void onRequestCommandCompletions( RequestCommandCompletionsC2SPacket packet ) { }

        @Override
        public void sendPacket( Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> listener ) { }

        @Override
        public void onVehicleMove( VehicleMoveC2SPacket move ) { }
    }

    private static class FakeConnection extends ClientConnection
    {
        FakeConnection()
        {
            super( NetworkSide.CLIENTBOUND );
        }

        @Override
        public void channelActive( ChannelHandlerContext active )
        {
        }

        @Override
        public void setState( NetworkState state )
        {
        }

        @Override
        public void disconnect( Text message )
        {
        }

        @Override
        public void exceptionCaught( ChannelHandlerContext context, Throwable err )
        {
        }

        @Override
        protected void method_10770( ChannelHandlerContext context, Packet<?> packet )
        {
        }

        @Override
        public void tick()
        {
        }

        @Override
        public void setupEncryption( SecretKey key )
        {
        }

        @Override
        public void disableAutoRead()
        {
        }

        @Override
        public void setMinCompressedSize( int size )
        {
        }

        @Override
        public void send( Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> listener )
        {
        }
    }
}
