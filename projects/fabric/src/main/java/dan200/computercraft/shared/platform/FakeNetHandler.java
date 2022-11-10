/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.platform;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import javax.annotation.Nullable;

class FakeNetHandler extends ServerGamePacketListenerImpl {
    protected static final Connection CONNECTION = new Connection(PacketFlow.CLIENTBOUND);

    FakeNetHandler(ServerPlayer player) {
        super(player.getLevel().getServer(), CONNECTION, player);
    }

    @Override
    public void tick() {
    }

    @Override
    public void resetPosition() {
    }

    @Override
    public void disconnect(Component textComponent) {
    }

    @Override
    public void handlePlayerInput(ServerboundPlayerInputPacket packet) {
    }

    @Override
    public void handleMoveVehicle(ServerboundMoveVehiclePacket packet) {
    }

    @Override
    public void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket packet) {
    }

    @Override
    public void handleRecipeBookSeenRecipePacket(ServerboundRecipeBookSeenRecipePacket packet) {
    }

    @Override
    public void handleRecipeBookChangeSettingsPacket(ServerboundRecipeBookChangeSettingsPacket packet) {
    }

    @Override
    public void handleSeenAdvancements(ServerboundSeenAdvancementsPacket packet) {
    }

    @Override
    public void handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket packet) {
    }

    @Override
    public void handleSetCommandBlock(ServerboundSetCommandBlockPacket packet) {
    }

    @Override
    public void handleSetCommandMinecart(ServerboundSetCommandMinecartPacket packet) {
    }

    @Override
    public void handlePickItem(ServerboundPickItemPacket packet) {
    }

    @Override
    public void handleRenameItem(ServerboundRenameItemPacket packet) {
    }

    @Override
    public void handleSetBeaconPacket(ServerboundSetBeaconPacket packet) {
    }

    @Override
    public void handleSetStructureBlock(ServerboundSetStructureBlockPacket packet) {
    }

    @Override
    public void handleSetJigsawBlock(ServerboundSetJigsawBlockPacket packet) {
    }

    @Override
    public void handleJigsawGenerate(ServerboundJigsawGeneratePacket packet) {
    }

    @Override
    public void handleSelectTrade(ServerboundSelectTradePacket packet) {
    }

    @Override
    public void handleEditBook(ServerboundEditBookPacket packet) {
    }

    @Override
    public void handleEntityTagQuery(ServerboundEntityTagQuery packet) {
    }

    @Override
    public void handleBlockEntityTagQuery(ServerboundBlockEntityTagQuery packet) {
    }

    @Override
    public void handleMovePlayer(ServerboundMovePlayerPacket packet) {
    }

    @Override
    public void handlePlayerAction(ServerboundPlayerActionPacket packet) {
    }

    @Override
    public void handleUseItemOn(ServerboundUseItemOnPacket packet) {
    }

    @Override
    public void handleUseItem(ServerboundUseItemPacket packet) {
    }

    @Override
    public void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket packet) {
    }

    @Override
    public void handleResourcePackResponse(ServerboundResourcePackPacket packet) {
    }

    @Override
    public void handlePaddleBoat(ServerboundPaddleBoatPacket packet) {
    }

    @Override
    public void handlePong(ServerboundPongPacket packet) {
    }

    @Override
    public void onDisconnect(Component reason) {
    }

    @Override
    public void ackBlockChangesUpTo(int i) {
    }

    @Override
    public void send(Packet<?> packet, @Nullable PacketSendListener packetSendListener) {
        super.send(packet, packetSendListener);
    }

    @Override
    public void handleSetCarriedItem(ServerboundSetCarriedItemPacket packet) {
    }

    @Override
    public void handleChat(ServerboundChatPacket packet) {
    }

    @Override
    public void handleChatCommand(ServerboundChatCommandPacket serverboundChatCommandPacket) {
    }

    @Override
    public void handleChatPreview(ServerboundChatPreviewPacket serverboundChatPreviewPacket) {
    }

    @Override
    public void handleChatAck(ServerboundChatAckPacket serverboundChatAckPacket) {
    }

    @Override
    public void handleAnimate(ServerboundSwingPacket packet) {
    }

    @Override
    public void handlePlayerCommand(ServerboundPlayerCommandPacket packet) {
    }

    @Override
    public void addPendingMessage(PlayerChatMessage playerChatMessage) {
    }

    @Override
    public void handleInteract(ServerboundInteractPacket packet) {
    }

    @Override
    public void handleClientCommand(ServerboundClientCommandPacket packet) {
    }

    @Override
    public void handleContainerClose(ServerboundContainerClosePacket packet) {
    }

    @Override
    public void handleContainerClick(ServerboundContainerClickPacket packet) {
    }

    @Override
    public void handlePlaceRecipe(ServerboundPlaceRecipePacket packet) {
    }

    @Override
    public void handleContainerButtonClick(ServerboundContainerButtonClickPacket packet) {
    }

    @Override
    public void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket packet) {
    }

    @Override
    public void handleSignUpdate(ServerboundSignUpdatePacket packet) {
    }

    @Override
    public void handleKeepAlive(ServerboundKeepAlivePacket packet) {
    }

    @Override
    public void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket packet) {
    }

    @Override
    public void handleClientInformation(ServerboundClientInformationPacket packet) {
    }

    @Override
    public void handleCustomPayload(ServerboundCustomPayloadPacket packet) {
    }

    @Override
    public void handleChangeDifficulty(ServerboundChangeDifficultyPacket packet) {
    }

    @Override
    public void handleLockDifficulty(ServerboundLockDifficultyPacket packet) {
    }
}
