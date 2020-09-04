/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command.text;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dan200.computercraft.shared.command.CommandUtils;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.client.ChatTableClientMessage;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class TableBuilder {
    private final int id;
    private final Text[] headers;
    private final ArrayList<Text[]> rows = new ArrayList<>();
    private int columns = -1;
    private int additional;

    public TableBuilder(int id, @Nonnull Text... headers) {
        if (id < 0) {
            throw new IllegalArgumentException("ID must be positive");
        }
        this.id = id;
        this.headers = headers;
        this.columns = headers.length;
    }

    public TableBuilder(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("ID must be positive");
        }
        this.id = id;
        this.headers = null;
    }

    public TableBuilder(int id, @Nonnull String... headers) {
        if (id < 0) {
            throw new IllegalArgumentException("ID must be positive");
        }
        this.id = id;
        this.headers = new Text[headers.length];
        this.columns = headers.length;

        for (int i = 0; i < headers.length; i++) {
            this.headers[i] = ChatHelpers.header(headers[i]);
        }
    }

    public void row(@Nonnull Text... row) {
        if (this.columns == -1) {
            this.columns = row.length;
        }
        if (row.length != this.columns) {
            throw new IllegalArgumentException("Row is the incorrect length");
        }
        this.rows.add(row);
    }

    /**
     * Get the unique identifier for this table type.
     *
     * When showing a table within Minecraft, previous instances of this table with the same ID will be removed from chat.
     *
     * @return This table's type.
     */
    public int getId() {
        return this.id;
    }

    /**
     * Get the number of columns for this table.
     *
     * This will be the same as {@link #getHeaders()}'s length if it is is non-{@code null}, otherwise the length of the first column.
     *
     * @return The number of columns.
     */
    public int getColumns() {
        return this.columns;
    }

    @Nullable
    public Text[] getHeaders() {
        return this.headers;
    }

    @Nonnull
    public List<Text[]> getRows() {
        return this.rows;
    }

    public int getAdditional() {
        return this.additional;
    }

    public void setAdditional(int additional) {
        this.additional = additional;
    }

    public void display(ServerCommandSource source) {
        if (CommandUtils.isPlayer(source)) {
            this.trim(18);
            NetworkHandler.sendToPlayer((ServerPlayerEntity) source.getEntity(), new ChatTableClientMessage(this));
        } else {
            this.trim(100);
            new ServerTableFormatter(source).display(this);
        }
    }

    /**
     * Trim this table to a given height.
     *
     * @param height The desired height.
     */
    public void trim(int height) {
        if (this.rows.size() > height) {
            this.additional += this.rows.size() - height - 1;
            this.rows.subList(height - 1, this.rows.size())
                     .clear();
        }
    }
}
