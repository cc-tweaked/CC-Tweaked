/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.client.ClientTableFormatter;
import dan200.computercraft.shared.command.text.TableBuilder;
import dan200.computercraft.shared.network.NetworkMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;

public class ChatTableClientMessage implements NetworkMessage {
    private static final int MAX_LEN = 16;
    private final TableBuilder table;

    public ChatTableClientMessage(TableBuilder table) {
        if (table.getColumns() < 0) throw new IllegalStateException("Cannot send an empty table");
        this.table = table;
    }

    public ChatTableClientMessage(@Nonnull FriendlyByteBuf buf) {
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
    public void toBytes(@Nonnull FriendlyByteBuf buf) {
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
    public void handle(NetworkEvent.Context context) {
        ClientTableFormatter.INSTANCE.display(table);
    }
}
