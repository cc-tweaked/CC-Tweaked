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
    public void onDisconnect( @Nonnull ITextComponent reason )
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
    public void processInput( @Nonnull CInputPacket packet )
    {
    }

    @Override
    public void processVehicleMove( @Nonnull CMoveVehiclePacket packet )
    {
    }

    @Override
    public void processConfirmTeleport( @Nonnull CConfirmTeleportPacket packet )
    {
    }

    @Override
    public void handleRecipeBookUpdate( @Nonnull CRecipeInfoPacket packet )
    {
    }

    @Override
    public void handleSeenAdvancements( @Nonnull CSeenAdvancementsPacket packet )
    {
    }

    @Override
    public void processTabComplete( @Nonnull CTabCompletePacket packet )
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
    public void processPickItem( @Nonnull CPickItemPacket packet )
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
    public void processSelectTrade( @Nonnull CSelectTradePacket packet )
    {
    }

    @Override
    public void processEditBook( @Nonnull CEditBookPacket packet )
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
    public void processPlayer( @Nonnull CPlayerPacket packet )
    {
    }

    @Override
    public void processPlayerDigging( @Nonnull CPlayerDiggingPacket packet )
    {
    }

    @Override
    public void processTryUseItemOnBlock( @Nonnull CPlayerTryUseItemOnBlockPacket packet )
    {
    }

    @Override
    public void processTryUseItem( @Nonnull CPlayerTryUseItemPacket packet )
    {
    }

    @Override
    public void handleSpectate( @Nonnull CSpectatePacket packet )
    {
    }

    @Override
    public void handleResourcePackStatus( @Nonnull CResourcePackStatusPacket packet )
    {
    }

    @Override
    public void processSteerBoat( @Nonnull CSteerBoatPacket packet )
    {
    }

    @Override
    public void processHeldItemChange( @Nonnull CHeldItemChangePacket packet )
    {
    }

    @Override
    public void processChatMessage( @Nonnull CChatMessagePacket packet )
    {
    }

    @Override
    public void handleAnimation( @Nonnull CAnimateHandPacket packet )
    {
    }

    @Override
    public void processEntityAction( @Nonnull CEntityActionPacket packet )
    {
    }

    @Override
    public void processUseEntity( @Nonnull CUseEntityPacket packet )
    {
    }

    @Override
    public void processClientStatus( @Nonnull CClientStatusPacket packet )
    {
    }

    @Override
    public void processCloseWindow( @Nonnull CCloseWindowPacket packet )
    {
    }

    @Override
    public void processClickWindow( @Nonnull CClickWindowPacket packet )
    {
    }

    @Override
    public void processPlaceRecipe( @Nonnull CPlaceRecipePacket packet )
    {
    }

    @Override
    public void processEnchantItem( @Nonnull CEnchantItemPacket packet )
    {
    }

    @Override
    public void processCreativeInventoryAction( @Nonnull CCreativeInventoryActionPacket packet )
    {
    }

    @Override
    public void processConfirmTransaction( @Nonnull CConfirmTransactionPacket packet )
    {
    }

    @Override
    public void processUpdateSign( @Nonnull CUpdateSignPacket packet )
    {
    }

    @Override
    public void processKeepAlive( @Nonnull CKeepAlivePacket packet )
    {
    }

    @Override
    public void processPlayerAbilities( @Nonnull CPlayerAbilitiesPacket packet )
    {
    }

    @Override
    public void processClientSettings( @Nonnull CClientSettingsPacket packet )
    {
    }

    @Override
    public void processCustomPayload( @Nonnull CCustomPayloadPacket packet )
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
        public void channelActive( @Nonnull ChannelHandlerContext context )
        {
        }

        @Override
        public void setConnectionState( @Nonnull ProtocolType state )
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
        public void setNetHandler( @Nonnull INetHandler handler )
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
        public void enableEncryption( @Nonnull SecretKey key )
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
