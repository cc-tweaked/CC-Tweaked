// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.command.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

import static dan200.computercraft.shared.command.text.ChatHelpers.coloured;

public interface TableFormatter {
    Component SEPARATOR = coloured("| ", ChatFormatting.GRAY);
    Component HEADER = coloured("=", ChatFormatting.GRAY);

    /**
     * Get additional padding for the component.
     *
     * @param component The component to pad
     * @param width     The desired width for the component
     * @return The padding for this component, or {@code null} if none is needed.
     */
    @Nullable
    Component getPadding(Component component, int width);

    /**
     * Get the minimum padding between each column.
     *
     * @return The minimum padding.
     */
    int getColumnPadding();

    int getWidth(Component component);

    void writeLine(String label, Component component);

    default void display(TableBuilder table) {
        if (table.getColumns() <= 0) return;

        var id = table.getId();
        var columns = table.getColumns();
        var maxWidths = new int[columns];

        var headers = table.getHeaders();
        if (headers != null) {
            for (var i = 0; i < columns; i++) maxWidths[i] = getWidth(headers[i]);
        }

        for (var row : table.getRows()) {
            for (var i = 0; i < row.length; i++) {
                var width = getWidth(row[i]);
                if (width > maxWidths[i]) maxWidths[i] = width;
            }
        }

        // Add a small amount of padding after each column
        {
            var padding = getColumnPadding();
            for (var i = 0; i < maxWidths.length - 1; i++) maxWidths[i] += padding;
        }

        // And compute the total width
        var totalWidth = (columns - 1) * getWidth(SEPARATOR);
        for (var x : maxWidths) totalWidth += x;

        if (headers != null) {
            var line = Component.literal("");
            for (var i = 0; i < columns - 1; i++) {
                line.append(headers[i]);
                var padding = getPadding(headers[i], maxWidths[i]);
                if (padding != null) line.append(padding);
                line.append(SEPARATOR);
            }
            line.append(headers[columns - 1]);

            writeLine(id, line);

            // Write a separator line. We round the width up rather than down to make
            // it a tad prettier.
            var rowCharWidth = getWidth(HEADER);
            var rowWidth = totalWidth / rowCharWidth + (totalWidth % rowCharWidth == 0 ? 0 : 1);
            writeLine(id, coloured(StringUtils.repeat(HEADER.getString(), rowWidth), ChatFormatting.GRAY));
        }

        for (var row : table.getRows()) {
            var line = Component.literal("");
            for (var i = 0; i < columns - 1; i++) {
                line.append(row[i]);
                var padding = getPadding(row[i], maxWidths[i]);
                if (padding != null) line.append(padding);
                line.append(SEPARATOR);
            }
            line.append(row[columns - 1]);
            writeLine(id, line);
        }

        if (table.getAdditional() > 0) {
            writeLine(id, Component.translatable("commands.computercraft.generic.additional_rows", table.getAdditional()).withStyle(ChatFormatting.AQUA));
        }
    }
}
