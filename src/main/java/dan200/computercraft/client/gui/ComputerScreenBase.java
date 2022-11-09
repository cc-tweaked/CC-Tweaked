/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.client.gui.widgets.ComputerSidebar;
import dan200.computercraft.client.gui.widgets.DynamicImageButton;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.client.platform.ClientPlatformHelper;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.InputHandler;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.computer.upload.FileUpload;
import dan200.computercraft.shared.computer.upload.UploadResult;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.network.server.UploadFileMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static dan200.computercraft.core.util.Nullability.assertNonNull;

public abstract class ComputerScreenBase<T extends ContainerComputerBase> extends AbstractContainerScreen<T> {
    private static final Logger LOG = LoggerFactory.getLogger(ComputerScreenBase.class);

    private static final Component OK = Component.translatable("gui.ok");
    private static final Component NO_RESPONSE_TITLE = Component.translatable("gui.computercraft.upload.no_response");
    private static final Component NO_RESPONSE_MSG = Component.translatable("gui.computercraft.upload.no_response.msg",
        Component.literal("import").withStyle(ChatFormatting.DARK_GRAY));

    protected @Nullable WidgetTerminal terminal;
    protected Terminal terminalData;
    protected final ComputerFamily family;
    protected final InputHandler input;

    protected final int sidebarYOffset;

    private long uploadNagDeadline = Long.MAX_VALUE;
    private final ItemStack displayStack;

    public ComputerScreenBase(T container, Inventory player, Component title, int sidebarYOffset) {
        super(container, player, title);
        terminalData = container.getTerminal();
        family = container.getFamily();
        displayStack = container.getDisplayStack();
        input = new ClientInputHandler(menu);
        this.sidebarYOffset = sidebarYOffset;
    }

    protected abstract WidgetTerminal createTerminal();

    protected final WidgetTerminal getTerminal() {
        if (terminal == null) throw new IllegalStateException("Screen has not been initialised yet");
        return terminal;
    }

    @Override
    protected void init() {
        super.init();
        minecraft.keyboardHandler.setSendRepeatsToGui(true);

        terminal = addRenderableWidget(createTerminal());
        ComputerSidebar.addButtons(this, menu::isOn, input, this::addRenderableWidget, leftPos, topPos + sidebarYOffset);
        setFocused(terminal);
    }

    @Override
    public void removed() {
        super.removed();
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        getTerminal().update();

        if (uploadNagDeadline != Long.MAX_VALUE && Util.getNanos() >= uploadNagDeadline) {
            new ItemToast(minecraft, displayStack, NO_RESPONSE_TITLE, NO_RESPONSE_MSG, ItemToast.TRANSFER_NO_RESPONSE_TOKEN)
                .showOrReplace(minecraft.getToasts());
            uploadNagDeadline = Long.MAX_VALUE;
        }
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        // Forward the tab key to the terminal, rather than moving between controls.
        if (key == GLFW.GLFW_KEY_TAB && getFocused() != null && getFocused() == terminal) {
            return getFocused().keyPressed(key, scancode, modifiers);
        }

        return super.keyPressed(key, scancode, modifiers);
    }


    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(stack);
        super.render(stack, mouseX, mouseY, partialTicks);
        renderTooltip(stack, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        var changed = super.mouseClicked(x, y, button);
        // Clicking the terminate/shutdown button steals focus, which means then pressing "enter" will click the button
        // again. Restore the focus to the terminal in these cases.
        if (getFocused() instanceof DynamicImageButton) setFocused(terminal);
        return changed;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double deltaX, double deltaY) {
        return (getFocused() != null && getFocused().mouseDragged(x, y, button, deltaX, deltaY))
            || super.mouseDragged(x, y, button, deltaX, deltaY);
    }


    @Override
    protected void renderLabels(PoseStack transform, int mouseX, int mouseY) {
        // Skip rendering labels.
    }

    @Override
    public void onFilesDrop(List<Path> files) {
        if (files.isEmpty()) return;

        if (!menu.isOn()) {
            alert(UploadResult.FAILED_TITLE, UploadResult.COMPUTER_OFF_MSG);
            return;
        }

        long size = 0;

        List<FileUpload> toUpload = new ArrayList<>();
        for (var file : files) {
            // TODO: Recurse directories? If so, we probably want to shunt this off-thread.
            if (!Files.isRegularFile(file)) continue;

            try (var sbc = Files.newByteChannel(file)) {
                var fileSize = sbc.size();
                if (fileSize > UploadFileMessage.MAX_SIZE || (size += fileSize) >= UploadFileMessage.MAX_SIZE) {
                    alert(UploadResult.FAILED_TITLE, UploadResult.TOO_MUCH_MSG);
                    return;
                }

                var name = file.getFileName().toString();
                if (name.length() > UploadFileMessage.MAX_FILE_NAME) {
                    alert(UploadResult.FAILED_TITLE, Component.translatable("gui.computercraft.upload.failed.name_too_long"));
                    return;
                }

                var buffer = ByteBuffer.allocateDirect((int) fileSize);
                sbc.read(buffer);
                buffer.flip();

                var digest = FileUpload.getDigest(buffer);
                if (digest == null) {
                    alert(UploadResult.FAILED_TITLE, Component.translatable("gui.computercraft.upload.failed.corrupted"));
                    return;
                }

                toUpload.add(new FileUpload(name, buffer, digest));
            } catch (IOException e) {
                LOG.error("Failed uploading files", e);
                alert(UploadResult.FAILED_TITLE, Component.translatable("gui.computercraft.upload.failed.generic", "Cannot compute checksum"));
            }
        }

        if (toUpload.size() > UploadFileMessage.MAX_FILES) {
            alert(UploadResult.FAILED_TITLE, Component.translatable("gui.computercraft.upload.failed.too_many_files"));
            return;
        }

        if (toUpload.size() > 0) UploadFileMessage.send(menu, toUpload, ClientPlatformHelper.get()::sendToServer);
    }

    public void uploadResult(UploadResult result, @Nullable Component message) {
        switch (result) {
            case QUEUED -> {
                if (Config.uploadNagDelay > 0) {
                    uploadNagDeadline = Util.getNanos() + TimeUnit.SECONDS.toNanos(Config.uploadNagDelay);
                }
            }
            case CONSUMED -> uploadNagDeadline = Long.MAX_VALUE;
            case ERROR -> alert(UploadResult.FAILED_TITLE, assertNonNull(message));
        }
    }

    private void alert(Component title, Component message) {
        OptionScreen.show(minecraft, title, message,
            Collections.singletonList(OptionScreen.newButton(OK, b -> minecraft.setScreen(this))),
            () -> minecraft.setScreen(this)
        );
    }
}
