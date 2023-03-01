// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.upload;

import net.minecraft.network.chat.Component;

public enum UploadResult {
    QUEUED,
    CONSUMED,
    ERROR;

    public static final Component FAILED_TITLE = Component.translatable("gui.computercraft.upload.failed");
    public static final Component COMPUTER_OFF_MSG = Component.translatable("gui.computercraft.upload.failed.computer_off");
    public static final Component TOO_MUCH_MSG = Component.translatable("gui.computercraft.upload.failed.too_much");
}
