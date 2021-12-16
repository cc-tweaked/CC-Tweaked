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
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Cipher;

public class FakeNetHandler extends ServerGamePacketListenerImpl
{
    public FakeNetHandler( @Nonnull FakePlayer player )
    {
        super( player.getLevel()
            .getServer(), new FakeNetworkManager(), player );
    }

    @Override
    public void tick()
    {
    }

    @Override
    public void disconnect( @Nonnull Component reason )
    {
    }

    @Override
    public void handlePlayerInput( @Nonnull ServerboundPlayerInputPacket packet )
    {
    }

    @Override
    public void handleMoveVehicle( @Nonnull ServerboundMoveVehiclePacket packet )
    {
    }

    @Override
    public void handleAcceptTeleportPacket( @Nonnull ServerboundAcceptTeleportationPacket packet )
    {
    }

    @Override
    public void handleSeenAdvancements( @Nonnull ServerboundSeenAdvancementsPacket packet )
    {
    }

    @Override
    public void handleCustomCommandSuggestions( @Nonnull ServerboundCommandSuggestionPacket packet )
    {
    }

    @Override
    public void handleSetCommandBlock( @Nonnull ServerboundSetCommandBlockPacket packet )
    {
    }

    @Override
    public void handleSetCommandMinecart( @Nonnull ServerboundSetCommandMinecartPacket packet )
    {
    }

    @Override
    public void handlePickItem( @Nonnull ServerboundPickItemPacket packet )
    {
    }

    @Override
    public void handleRenameItem( @Nonnull ServerboundRenameItemPacket packet )
    {
    }

    @Override
    public void handleSetBeaconPacket( @Nonnull ServerboundSetBeaconPacket packet )
    {
    }

    @Override
    public void handleSetStructureBlock( @Nonnull ServerboundSetStructureBlockPacket packet )
    {
    }

    @Override
    public void handleSetJigsawBlock( @Nonnull ServerboundSetJigsawBlockPacket packet )
    {
    }

    @Override
    public void handleJigsawGenerate( ServerboundJigsawGeneratePacket packet )
    {
    }

    @Override
    public void handleSelectTrade( ServerboundSelectTradePacket packet )
    {
    }

    @Override
    public void handleEditBook( @Nonnull ServerboundEditBookPacket packet )
    {
    }

    @Override
    public void handleRecipeBookSeenRecipePacket( ServerboundRecipeBookSeenRecipePacket packet )
    {
    }

    @Override
    public void handleRecipeBookChangeSettingsPacket( ServerboundRecipeBookChangeSettingsPacket packet )
    {
        super.handleRecipeBookChangeSettingsPacket( packet );
    }

    @Override
    public void handleEntityTagQuery( @Nonnull ServerboundEntityTagQuery packet )
    {
    }

    @Override
    public void handleBlockEntityTagQuery( @Nonnull ServerboundBlockEntityTagQuery packet )
    {
    }

    @Override
    public void handleMovePlayer( @Nonnull ServerboundMovePlayerPacket packet )
    {
    }

    @Override
    public void handlePlayerAction( @Nonnull ServerboundPlayerActionPacket packet )
    {
    }

    @Override
    public void handleUseItemOn( @Nonnull ServerboundUseItemOnPacket packet )
    {
    }

    @Override
    public void handleUseItem( @Nonnull ServerboundUseItemPacket packet )
    {
    }

    @Override
    public void handleTeleportToEntityPacket( @Nonnull ServerboundTeleportToEntityPacket packet )
    {
    }

    @Override
    public void handleResourcePackResponse( @Nonnull ServerboundResourcePackPacket packet )
    {
    }

    @Override
    public void handlePaddleBoat( @Nonnull ServerboundPaddleBoatPacket packet )
    {
    }

    @Override
    public void onDisconnect( @Nonnull Component reason )
    {
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
    public void handleSetCarriedItem( @Nonnull ServerboundSetCarriedItemPacket packet )
    {
    }

    @Override
    public void handleChat( @Nonnull ServerboundChatPacket packet )
    {
    }

    @Override
    public void handleAnimate( @Nonnull ServerboundSwingPacket packet )
    {
    }

    @Override
    public void handlePlayerCommand( @Nonnull ServerboundPlayerCommandPacket packet )
    {
    }

    @Override
    public void handleInteract( @Nonnull ServerboundInteractPacket packet )
    {
    }

    @Override
    public void handleClientCommand( @Nonnull ServerboundClientCommandPacket packet )
    {
    }

    @Override
    public void handleContainerClose( ServerboundContainerClosePacket packet )
    {
    }

    @Override
    public void handleContainerClick( ServerboundContainerClickPacket packet )
    {
    }

    @Override
    public void handlePlaceRecipe( @Nonnull ServerboundPlaceRecipePacket packet )
    {
    }

    @Override
    public void handleContainerButtonClick( @Nonnull ServerboundContainerButtonClickPacket packet )
    {
    }

    @Override
    public void handleSetCreativeModeSlot( @Nonnull ServerboundSetCreativeModeSlotPacket packet )
    {
    }

    //    @Override
    //    public void onConfirmScreenAction( ConfirmScreenActionC2SPacket packet )
    //    {
    //    }

    @Override
    public void handleSignUpdate( @Nonnull ServerboundSignUpdatePacket packet )
    {
    }

    @Override
    public void handleKeepAlive( @Nonnull ServerboundKeepAlivePacket packet )
    {
    }

    @Override
    public void handlePlayerAbilities( @Nonnull ServerboundPlayerAbilitiesPacket packet )
    {
    }

    @Override
    public void handleClientInformation( @Nonnull ServerboundClientInformationPacket packet )
    {
    }

    @Override
    public void handleCustomPayload( @Nonnull ServerboundCustomPayloadPacket packet )
    {
    }

    @Override
    public void handleChangeDifficulty( @Nonnull ServerboundChangeDifficultyPacket packet )
    {
    }

    @Override
    public void handleLockDifficulty( @Nonnull ServerboundLockDifficultyPacket packet )
    {
    }

    private static class FakeNetworkManager extends Connection
    {
        private PacketListener handler;
        private Component closeReason;

        FakeNetworkManager()
        {
            super( PacketFlow.CLIENTBOUND );
        }

        @Override
        public void channelActive( @Nonnull ChannelHandlerContext context )
        {
        }

        @Override
        public void setProtocol( @Nonnull ConnectionProtocol state )
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
        public void setListener( @Nonnull PacketListener handler )
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
        public void disconnect( @Nonnull Component message )
        {
            closeReason = message;
        }

        @Override
        public void setEncryptionKey( Cipher cipher, Cipher cipher2 )
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
        public Component getDisconnectedReason()
        {
            return closeReason;
        }

        @Override
        public void setReadOnly()
        {
        }
    }
}
