/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import dan200.computercraft.api.turtle.FakePlayer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Cipher;

public class FakeNetHandler extends ServerPlayNetworkHandler
{
    public FakeNetHandler( @Nonnull FakePlayer player )
    {
        super( player.getServerWorld()
            .getServer(), new FakeNetworkManager(), player );
    }

    @Override
    public void tick()
    {
    }

    @Override
    public void disconnect( @Nonnull Text reason )
    {
    }

    @Override
    public void onPlayerInput( @Nonnull PlayerInputC2SPacket packet )
    {
    }

    @Override
    public void onVehicleMove( @Nonnull VehicleMoveC2SPacket packet )
    {
    }

    @Override
    public void onTeleportConfirm( @Nonnull TeleportConfirmC2SPacket packet )
    {
    }

    @Override
    public void onAdvancementTab( @Nonnull AdvancementTabC2SPacket packet )
    {
    }

    @Override
    public void onRequestCommandCompletions( @Nonnull RequestCommandCompletionsC2SPacket packet )
    {
    }

    @Override
    public void onUpdateCommandBlock( @Nonnull UpdateCommandBlockC2SPacket packet )
    {
    }

    @Override
    public void onUpdateCommandBlockMinecart( @Nonnull UpdateCommandBlockMinecartC2SPacket packet )
    {
    }

    @Override
    public void onPickFromInventory( @Nonnull PickFromInventoryC2SPacket packet )
    {
    }

    @Override
    public void onRenameItem( @Nonnull RenameItemC2SPacket packet )
    {
    }

    @Override
    public void onUpdateBeacon( @Nonnull UpdateBeaconC2SPacket packet )
    {
    }

    @Override
    public void onStructureBlockUpdate( @Nonnull UpdateStructureBlockC2SPacket packet )
    {
    }

    @Override
    public void onJigsawUpdate( @Nonnull UpdateJigsawC2SPacket packet )
    {
    }

    @Override
    public void onJigsawGenerating( JigsawGeneratingC2SPacket packet )
    {
    }

    @Override
    public void onMerchantTradeSelect( SelectMerchantTradeC2SPacket packet )
    {
    }

    @Override
    public void onBookUpdate( @Nonnull BookUpdateC2SPacket packet )
    {
    }

    @Override
    public void onRecipeBookData( RecipeBookDataC2SPacket packet )
    {
    }

    @Override
    public void onRecipeCategoryOptions( RecipeCategoryOptionsC2SPacket packet )
    {
        super.onRecipeCategoryOptions( packet );
    }

    @Override
    public void onQueryEntityNbt( @Nonnull QueryEntityNbtC2SPacket packet )
    {
    }

    @Override
    public void onQueryBlockNbt( @Nonnull QueryBlockNbtC2SPacket packet )
    {
    }

    @Override
    public void onPlayerMove( @Nonnull PlayerMoveC2SPacket packet )
    {
    }

    @Override
    public void onPlayerAction( @Nonnull PlayerActionC2SPacket packet )
    {
    }

    @Override
    public void onPlayerInteractBlock( @Nonnull PlayerInteractBlockC2SPacket packet )
    {
    }

    @Override
    public void onPlayerInteractItem( @Nonnull PlayerInteractItemC2SPacket packet )
    {
    }

    @Override
    public void onSpectatorTeleport( @Nonnull SpectatorTeleportC2SPacket packet )
    {
    }

    @Override
    public void onResourcePackStatus( @Nonnull ResourcePackStatusC2SPacket packet )
    {
    }

    @Override
    public void onBoatPaddleState( @Nonnull BoatPaddleStateC2SPacket packet )
    {
    }

    @Override
    public void onDisconnected( @Nonnull Text reason )
    {
    }

    @Override
    public void sendPacket( @Nonnull Packet<?> packet )
    {
    }

    @Override
    public void sendPacket( @Nonnull Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> whenSent )
    {
    }

    @Override
    public void onUpdateSelectedSlot( @Nonnull UpdateSelectedSlotC2SPacket packet )
    {
    }

    @Override
    public void onGameMessage( @Nonnull ChatMessageC2SPacket packet )
    {
    }

    @Override
    public void onHandSwing( @Nonnull HandSwingC2SPacket packet )
    {
    }

    @Override
    public void onClientCommand( @Nonnull ClientCommandC2SPacket packet )
    {
    }

    @Override
    public void onPlayerInteractEntity( @Nonnull PlayerInteractEntityC2SPacket packet )
    {
    }

    @Override
    public void onClientStatus( @Nonnull ClientStatusC2SPacket packet )
    {
    }

    @Override
    public void onCloseHandledScreen( CloseHandledScreenC2SPacket packet )
    {
    }

    @Override
    public void onClickSlot( ClickSlotC2SPacket packet )
    {
    }

    @Override
    public void onCraftRequest( @Nonnull CraftRequestC2SPacket packet )
    {
    }

    @Override
    public void onButtonClick( @Nonnull ButtonClickC2SPacket packet )
    {
    }

    @Override
    public void onCreativeInventoryAction( @Nonnull CreativeInventoryActionC2SPacket packet )
    {
    }

    //    @Override
    //    public void onConfirmScreenAction( ConfirmScreenActionC2SPacket packet )
    //    {
    //    }

    @Override
    public void onSignUpdate( @Nonnull UpdateSignC2SPacket packet )
    {
    }

    @Override
    public void onKeepAlive( @Nonnull KeepAliveC2SPacket packet )
    {
    }

    @Override
    public void onPlayerAbilities( @Nonnull UpdatePlayerAbilitiesC2SPacket packet )
    {
    }

    @Override
    public void onClientSettings( @Nonnull ClientSettingsC2SPacket packet )
    {
    }

    @Override
    public void onCustomPayload( @Nonnull CustomPayloadC2SPacket packet )
    {
    }

    @Override
    public void onUpdateDifficulty( @Nonnull UpdateDifficultyC2SPacket packet )
    {
    }

    @Override
    public void onUpdateDifficultyLock( @Nonnull UpdateDifficultyLockC2SPacket packet )
    {
    }

    private static class FakeNetworkManager extends ClientConnection
    {
        private PacketListener handler;
        private Text closeReason;

        FakeNetworkManager()
        {
            super( NetworkSide.CLIENTBOUND );
        }

        @Override
        public void channelActive( @Nonnull ChannelHandlerContext context )
        {
        }

        @Override
        public void setState( @Nonnull NetworkState state )
        {
        }

        @Override
        public void channelInactive( @Nonnull ChannelHandlerContext context )
        {
        }

        @Override
        public void exceptionCaught( @Nonnull ChannelHandlerContext context, @Nonnull Throwable err )
        {
        }

        @Override
        protected void channelRead0( @Nonnull ChannelHandlerContext context, @Nonnull Packet<?> packet )
        {
        }

        @Override
        public void setPacketListener( @Nonnull PacketListener handler )
        {
            this.handler = handler;
        }

        @Override
        public void send( @Nonnull Packet<?> packet )
        {
        }

        @Override
        public void send( @Nonnull Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> whenSent )
        {
        }

        @Override
        public void tick()
        {
        }

        @Override
        public void disconnect( @Nonnull Text message )
        {
            closeReason = message;
        }

        @Override
        public void setupEncryption( Cipher cipher, Cipher cipher2 )
        {
        }

        @Nonnull
        @Override
        public PacketListener getPacketListener()
        {
            return handler;
        }

        @Nullable
        @Override
        public Text getDisconnectReason()
        {
            return closeReason;
        }

        @Override
        public void disableAutoRead()
        {
        }
    }
}
