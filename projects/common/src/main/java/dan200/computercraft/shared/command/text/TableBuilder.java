// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.command.text;

import dan200.computercraft.core.util.Nullability;
import dan200.computercraft.shared.command.CommandUtils;
import dan200.computercraft.shared.network.client.ChatTableClientMessage;
import dan200.computercraft.shared.network.server.ServerNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TableBuilder {
    private final String id;
    private int columns = -1;
    private final @Nullable Component[] headers;
    private final ArrayList<Component[]> rows = new ArrayList<>();
    private int additional;

    public TableBuilder(String id, Component... headers) {
        this.id = id;
        this.headers = headers;
        columns = headers.length;
    }

    public TableBuilder(String id) {
        this.id = id;
        headers = null;
    }

    public TableBuilder(String id, String... headers) {
        this.id = id;
        this.headers = new Component[headers.length];
        columns = headers.length;

        for (var i = 0; i < headers.length; i++) this.headers[i] = ChatHelpers.header(headers[i]);
    }

    public void row(Component... row) {
        if (columns == -1) columns = row.length;
        if (row.length != columns) throw new IllegalArgumentException("Row is the incorrect length");
        rows.add(row);
    }

    /**
     * Get the unique identifier for this table type.
     * <p>
     * When showing a table within Minecraft, previous instances of this table with
     * the same ID will be removed from chat.
     *
     * @return This table's type.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the number of columns for this table.
     * <p>
     * This will be the same as {@link #getHeaders()}'s length if it is non-{@code null},
     * otherwise the length of the first column.
     *
     * @return The number of columns.
     */
    public int getColumns() {
        return columns;
    }

    @Nullable
    public Component[] getHeaders() {
        return headers;
    }

    public List<Component[]> getRows() {
        return rows;
    }

    public int getAdditional() {
        return additional;
    }

    public void setAdditional(int additional) {
        this.additional = additional;
    }

    /**
     * Trim this table to a given height.
     *
     * @param height The desired height.
     */
    public void trim(int height) {
        if (rows.size() > height) {
            additional += rows.size() - height - 1;
            rows.subList(height - 1, rows.size()).clear();
        }
    }

    public void display(CommandSourceStack source) {
        if (CommandUtils.isPlayer(source)) {
            trim(18);
            var player = (ServerPlayer) Nullability.assertNonNull(source.getEntity());
            ServerNetworking.sendToPlayer(new ChatTableClientMessage(this), player);
        } else {
            trim(100);
            new ServerTableFormatter(source).display(this);
        }
    }
}
