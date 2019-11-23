/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
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
import javax.crypto.SecretKey;

public class FakeNetHandler extends ServerPlayNetHandler
{
    public FakeNetHandler( @Nonnull FakePlayer player )
    {
        super( player.getServerWorld().getServer(), new FakeNetworkManager(), player );
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
    public void onDisconnect( ITextComponent reason )
    {
    }

    @Override
    public void sendPacket( @Nonnull IPacket<?> packet )
    {
    }

    @Override
    public void sendPacket( @Nonnull IPacket<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> whenSent )
    {
    }

    @Override
    public void processInput( CInputPacket packet )
    {
    }

    @Override
    public void processVehicleMove( CMoveVehiclePacket packet )
    {
    }

    @Override
    public void processConfirmTeleport( CConfirmTeleportPacket packet )
    {
    }

    @Override
    public void handleRecipeBookUpdate( CRecipeInfoPacket packet )
    {
    }

    @Override
    public void handleSeenAdvancements( CSeenAdvancementsPacket packet )
    {
    }

    @Override
    public void processTabComplete( CTabCompletePacket packet )
    {
    }

    @Override
    public void processUpdateCommandBlock( @Nonnull CUpdateCommandBlockPacket packet )
    {
    }

    @Override
    public void processUpdateCommandMinecart( @Nonnull CUpdateMinecartCommandBlockPacket packet )
    {
    }

    @Override
    public void processPickItem( CPickItemPacket packet )
    {
    }

    @Override
    public void processRenameItem( @Nonnull CRenameItemPacket packet )
    {
    }

    @Override
    public void processUpdateBeacon( @Nonnull CUpdateBeaconPacket packet )
    {
    }

    @Override
    public void processUpdateStructureBlock( @Nonnull CUpdateStructureBlockPacket packet )
    {
    }

    @Override
    public void func_217262_a( @Nonnull CUpdateJigsawBlockPacket packet )
    {
    }

    @Override
    public void processSelectTrade( CSelectTradePacket packet )
    {
    }

    @Override
    public void processEditBook( CEditBookPacket packet )
    {
    }

    @Override
    public void processNBTQueryEntity( @Nonnull CQueryEntityNBTPacket packet )
    {
    }

    @Override
    public void processNBTQueryBlockEntity( @Nonnull CQueryTileEntityNBTPacket packet )
    {
    }

    @Override
    public void processPlayer( CPlayerPacket packet )
    {
    }

    @Override
    public void processPlayerDigging( CPlayerDiggingPacket packet )
    {
    }

    @Override
    public void processTryUseItemOnBlock( CPlayerTryUseItemOnBlockPacket packet )
    {
    }

    @Override
    public void processTryUseItem( CPlayerTryUseItemPacket packet )
    {
    }

    @Override
    public void handleSpectate( @Nonnull CSpectatePacket packet )
    {
    }

    @Override
    public void handleResourcePackStatus( CResourcePackStatusPacket packet )
    {
    }

    @Override
    public void processSteerBoat( @Nonnull CSteerBoatPacket packet )
    {
    }

    @Override
    public void processHeldItemChange( CHeldItemChangePacket packet )
    {
    }

    @Override
    public void processChatMessage( @Nonnull CChatMessagePacket packet )
    {
    }

    @Override
    public void handleAnimation( CAnimateHandPacket packet )
    {
    }

    @Override
    public void processEntityAction( CEntityActionPacket packet )
    {
    }

    @Override
    public void processUseEntity( CUseEntityPacket packet )
    {
    }

    @Override
    public void processClientStatus( CClientStatusPacket packet )
    {
    }

    @Override
    public void processCloseWindow( @Nonnull CCloseWindowPacket packet )
    {
    }

    @Override
    public void processClickWindow( CClickWindowPacket packet )
    {
    }

    @Override
    public void processPlaceRecipe( @Nonnull CPlaceRecipePacket packet )
    {
    }

    @Override
    public void processEnchantItem( CEnchantItemPacket packet )
    {
    }

    @Override
    public void processCreativeInventoryAction( @Nonnull CCreativeInventoryActionPacket packet )
    {
    }

    @Override
    public void processConfirmTransaction( CConfirmTransactionPacket packet )
    {
    }

    @Override
    public void processUpdateSign( CUpdateSignPacket packet )
    {
    }

    @Override
    public void processKeepAlive( @Nonnull CKeepAlivePacket packet )
    {
    }

    @Override
    public void processPlayerAbilities( CPlayerAbilitiesPacket packet )
    {
    }

    @Override
    public void processClientSettings( @Nonnull CClientSettingsPacket packet )
    {
    }

    @Override
    public void processCustomPayload( CCustomPayloadPacket packet )
    {
    }

    @Override
    public void func_217263_a( @Nonnull CSetDifficultyPacket packet )
    {
    }

    @Override
    public void func_217261_a( @Nonnull CLockDifficultyPacket packet )
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
        public void channelActive( ChannelHandlerContext context )
        {
        }

        @Override
        public void setConnectionState( @Nonnull ProtocolType state )
        {
        }

        @Override
        public void channelInactive( ChannelHandlerContext context )
        {
        }

        @Override
        public void exceptionCaught( ChannelHandlerContext context, @Nonnull Throwable err )
        {
        }

        @Override
        protected void channelRead0( ChannelHandlerContext context, @Nonnull IPacket<?> packet )
        {
        }

        @Override
        public void setNetHandler( INetHandler handler )
        {
            this.handler = handler;
        }

        @Override
        public void sendPacket( @Nonnull IPacket<?> packet )
        {
        }

        @Override
        public void sendPacket( @Nonnull IPacket<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> whenSent )
        {
        }

        @Override
        public void tick()
        {
        }

        @Override
        public void closeChannel( @Nonnull ITextComponent message )
        {
            this.closeReason = message;
        }

        @Override
        public void enableEncryption( SecretKey key )
        {
        }

        @Nonnull
        @Override
        public INetHandler getNetHandler()
        {
            return handler;
        }

        @Nullable
        @Override
        public ITextComponent getExitMessage()
        {
            return closeReason;
        }

        @Override
        public void disableAutoRead()
        {
        }

        @Override
        public void setCompressionThreshold( int threshold )
        {
        }
    }
}
