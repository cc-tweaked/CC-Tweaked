// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.network.client;

import dan200.computercraft.shared.command.text.TableBuilder;
import dan200.computercraft.shared.network.MessageType;
import dan200.computercraft.shared.network.NetworkMessage;
import dan200.computercraft.shared.network.NetworkMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;


public class ChatTableClientMessage implements NetworkMessage<ClientNetworkContext> {
    private static final int MAX_LEN = 16;
    private final TableBuilder table;

    public ChatTableClientMessage(TableBuilder table) {
        if (table.getColumns() < 0) throw new IllegalStateException("Cannot send an empty table");
        this.table = table;
    }

    public ChatTableClientMessage(FriendlyByteBuf buf) {
        var id = buf.readUtf(MAX_LEN);
        var columns = buf.readVarInt();
        TableBuilder table;
        if (buf.readBoolean()) {
            var headers = new Component[columns];
            for (var i = 0; i < columns; i++) headers[i] = buf.readComponent();
            table = new TableBuilder(id, headers);
        } else {
            table = new TableBuilder(id);
        }

        var rows = buf.readVarInt();
        for (var i = 0; i < rows; i++) {
            var row = new Component[columns];
            for (var j = 0; j < columns; j++) row[j] = buf.readComponent();
            table.row(row);
        }

        table.setAdditional(buf.readVarInt());
        this.table = table;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(table.getId(), MAX_LEN);
        buf.writeVarInt(table.getColumns());
        buf.writeBoolean(table.getHeaders() != null);
        if (table.getHeaders() != null) {
            for (var header : table.getHeaders()) buf.writeComponent(header);
        }

        buf.writeVarInt(table.getRows().size());
        for (var row : table.getRows()) {
            for (var column : row) buf.writeComponent(column);
        }

        buf.writeVarInt(table.getAdditional());
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.handleChatTable(table);
    }

    @Override
    public MessageType<ChatTableClientMessage> type() {
        return NetworkMessages.CHAT_TABLE;
    }
}
