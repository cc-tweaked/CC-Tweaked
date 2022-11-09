/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.command.text;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

public class ServerTableFormatter implements TableFormatter {
    private final CommandSourceStack source;

    public ServerTableFormatter(CommandSourceStack source) {
        this.source = source;
    }

    @Override
    @Nullable
    public Component getPadding(Component component, int width) {
        var extraWidth = width - getWidth(component);
        if (extraWidth <= 0) return null;
        return Component.literal(StringUtils.repeat(' ', extraWidth));
    }

    @Override
    public int getColumnPadding() {
        return 1;
    }

    @Override
    public int getWidth(Component component) {
        return component.getString().length();
    }

    @Override
    public void writeLine(String label, Component component) {
        source.sendSuccess(component, false);
    }
}
