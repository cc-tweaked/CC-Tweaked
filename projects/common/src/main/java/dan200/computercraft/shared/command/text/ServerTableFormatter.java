// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

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
        source.sendSuccess(() -> component, false);
    }
}
