/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.*;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.FakePlayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FakeNetHandler extends ServerPlayNetHandler
{
    public FakeNetHandler( @Nonnull FakePlayer player )
    {
        super( player.getLevel().getServer(), new FakeNetworkManager(), player );
    }

    @Override
    public void tick()
    {
    }

    @Override
    public void disconnect( @Nonnull ITextComponent reason )
    {
    }

    @Override
    public void onDisconnect( @Nonnull ITextComponent reason )
    {
    }

    @Override
    public void send( @Nonnull IPacket<?> packet )
    {
    }

    @Override
    public void send( @Nonnull IPacket<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> whenSent )
    {
    }

    @Override
    public void handlePlayerInput( @Nonnull CInputPacket packet )
    {
    }

    @Override
    public void handleMoveVehicle( @Nonnull CMoveVehiclePacket packet )
    {
    }

    @Override
    public void handleAcceptTeleportPacket( @Nonnull CConfirmTeleportPacket packet )
    {
    }

    @Override
    public void handleSeenAdvancements( @Nonnull CSeenAdvancementsPacket packet )
    {
    }

    @Override
    public void handleCustomCommandSuggestions( @Nonnull CTabCompletePacket packet )
    {
    }

    @Override
    public void handleSetCommandBlock( @Nonnull CUpdateCommandBlockPacket packet )
    {
    }

    @Override
    public void handleSetCommandMinecart( @Nonnull CUpdateMinecartCommandBlockPacket packet )
    {
    }

    @Override
    public void handlePickItem( @Nonnull CPickItemPacket packet )
    {
    }

    @Override
    public void handleRenameItem( @Nonnull CRenameItemPacket packet )
    {
    }

    @Override
    public void handleSetBeaconPacket( @Nonnull CUpdateBeaconPacket packet )
    {
    }

    @Override
    public void handleSetStructureBlock( @Nonnull CUpdateStructureBlockPacket packet )
    {
    }

    @Override
    public void handleSetJigsawBlock( @Nonnull CUpdateJigsawBlockPacket packet )
    {
    }

    @Override
    public void handleSelectTrade( @Nonnull CSelectTradePacket packet )
    {
    }

    @Override
    public void handleEditBook( @Nonnull CEditBookPacket packet )
    {
    }

    @Override
    public void handleEntityTagQuery( @Nonnull CQueryEntityNBTPacket packet )
    {
    }

    @Override
    public void handleBlockEntityTagQuery( @Nonnull CQueryTileEntityNBTPacket packet )
    {
    }

    @Override
    public void handleMovePlayer( @Nonnull CPlayerPacket packet )
    {
    }

    @Override
    public void handlePlayerAction( @Nonnull CPlayerDiggingPacket packet )
    {
    }

    @Override
    public void handleUseItemOn( @Nonnull CPlayerTryUseItemOnBlockPacket packet )
    {
    }

    @Override
    public void handleUseItem( @Nonnull CPlayerTryUseItemPacket packet )
    {
    }

    @Override
    public void handleTeleportToEntityPacket( @Nonnull CSpectatePacket packet )
    {
    }

    @Override
    public void handleResourcePackResponse( @Nonnull CResourcePackStatusPacket packet )
    {
    }

    @Override
    public void handlePaddleBoat( @Nonnull CSteerBoatPacket packet )
    {
    }

    @Override
    public void handleSetCarriedItem( @Nonnull CHeldItemChangePacket packet )
    {
    }

    @Override
    public void handleChat( @Nonnull CChatMessagePacket packet )
    {
    }

    @Override
    public void handleAnimate( @Nonnull CAnimateHandPacket packet )
    {
    }

    @Override
    public void handlePlayerCommand( @Nonnull CEntityActionPacket packet )
    {
    }

    @Override
    public void handleInteract( @Nonnull CUseEntityPacket packet )
    {
    }

    @Override
    public void handleClientCommand( @Nonnull CClientStatusPacket packet )
    {
    }

    @Override
    public void handleContainerClose( @Nonnull CCloseWindowPacket packet )
    {
    }

    @Override
    public void handleContainerClick( @Nonnull CClickWindowPacket packet )
    {
    }

    @Override
    public void handlePlaceRecipe( @Nonnull CPlaceRecipePacket packet )
    {
    }

    @Override
    public void handleContainerButtonClick( @Nonnull CEnchantItemPacket packet )
    {
    }

    @Override
    public void handleSetCreativeModeSlot( @Nonnull CCreativeInventoryActionPacket packet )
    {
    }

    @Override
    public void handleContainerAck( @Nonnull CConfirmTransactionPacket packet )
    {
    }

    @Override
    public void handleSignUpdate( @Nonnull CUpdateSignPacket packet )
    {
    }

    @Override
    public void handleKeepAlive( @Nonnull CKeepAlivePacket packet )
    {
    }

    @Override
    public void handlePlayerAbilities( @Nonnull CPlayerAbilitiesPacket packet )
    {
    }

    @Override
    public void handleClientInformation( @Nonnull CClientSettingsPacket packet )
    {
    }

    @Override
    public void handleCustomPayload( @Nonnull CCustomPayloadPacket packet )
    {
    }

    @Override
    public void handleChangeDifficulty( @Nonnull CSetDifficultyPacket packet )
    {
    }

    @Override
    public void handleLockDifficulty( @Nonnull CLockDifficultyPacket packet )
    {
    }

    private static class FakeNetworkManager extends NetworkManager
    {
        private INetHandler handler;
        private ITextComponent closeReason;

        FakeNetworkManager()
        {
            super( PacketDirection.CLIENTBOUND );
        }

        @Override
        public void channelActive( @Nonnull ChannelHandlerContext context )
        {
        }

        @Override
        public void setProtocol( @Nonnull ProtocolType state )
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
        protected void channelRead0( @Nonnull ChannelHandlerContext context, @Nonnull IPacket<?> packet )
        {
        }

        @Override
        public void setListener( @Nonnull INetHandler handler )
        {
            this.handler = handler;
        }

        @Override
        public void send( @Nonnull IPacket<?> packet )
        {
        }

        @Override
        public void send( @Nonnull IPacket<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> whenSent )
        {
        }

        @Override
        public void tick()
        {
        }

        @Override
        public void disconnect( @Nonnull ITextComponent message )
        {
            closeReason = message;
        }

        @Nonnull
        @Override
        public INetHandler getPacketListener()
        {
            return handler;
        }

        @Nullable
        @Override
        public ITextComponent getDisconnectedReason()
        {
            return closeReason;
        }

        @Override
        public void setReadOnly()
        {
        }

        @Override
        public void setupCompression( int threshold )
        {
        }
    }
}
