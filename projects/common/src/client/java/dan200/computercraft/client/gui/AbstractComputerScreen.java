// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.gui;

import dan200.computercraft.client.gui.widgets.ComputerSidebar;
import dan200.computercraft.client.gui.widgets.DynamicImageButton;
import dan200.computercraft.client.gui.widgets.TerminalWidget;
import dan200.computercraft.client.network.ClientNetworking;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.util.Nullability;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.InputHandler;
import dan200.computercraft.shared.computer.inventory.AbstractComputerMenu;
import dan200.computercraft.shared.computer.upload.FileUpload;
import dan200.computercraft.shared.computer.upload.UploadResult;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.network.server.UploadFileMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static dan200.computercraft.core.util.Nullability.assertNonNull;

/**
 * The base class of all screens with a computer terminal (i.e. {@link ComputerScreen}). This works with
 * {@link AbstractComputerMenu} to handle the common behaviour such as the terminal, input and file uploading.
 *
 * @param <T> The concrete type of the associated menu.
 */
public abstract class AbstractComputerScreen<T extends AbstractComputerMenu> extends AbstractContainerScreen<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractComputerScreen.class);

    private static final Component OK = Component.translatable("gui.ok");
    private static final Component NO_RESPONSE_TITLE = Component.translatable("gui.computercraft.upload.no_response");
    private static final Component NO_RESPONSE_MSG = Component.translatable("gui.computercraft.upload.no_response.msg",
        Component.literal("import").withStyle(ChatFormatting.DARK_GRAY));

    protected @Nullable TerminalWidget terminal;
    protected Terminal terminalData;
    protected final ComputerFamily family;
    protected final InputHandler input;

    protected final int sidebarYOffset;

    private long uploadNagDeadline = Long.MAX_VALUE;
    private final int uploadMaxSize;
    private final ItemStack displayStack;

    public AbstractComputerScreen(T container, Inventory player, Component title, int sidebarYOffset) {
        super(container, player, title);
        terminalData = container.getTerminal();
        family = container.getFamily();
        displayStack = container.getDisplayStack();
        uploadMaxSize = container.getUploadMaxSize();
        input = new ClientInputHandler(menu);
        this.sidebarYOffset = sidebarYOffset;
    }

    protected abstract TerminalWidget createTerminal();

    protected final TerminalWidget getTerminal() {
        if (terminal == null) throw new IllegalStateException("Screen has not been initialised yet");
        return terminal;
    }

    @Override
    protected void init() {
        super.init();

        terminal = addRenderableWidget(createTerminal());
        ComputerSidebar.addButtons(menu::isOn, input, this::addRenderableWidget, leftPos, topPos + sidebarYOffset);
        setFocused(terminal);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        getTerminal().update();

        if (uploadNagDeadline != Long.MAX_VALUE && Util.getNanos() >= uploadNagDeadline) {
            new ItemToast(minecraft(), displayStack, NO_RESPONSE_TITLE, NO_RESPONSE_MSG, ItemToast.TRANSFER_NO_RESPONSE_TOKEN)
                .showOrReplace(minecraft().getToasts());
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
    public boolean mouseReleased(double x, double y, int button) {
        // Reimplement ContainerEventHandler.mouseReleased, as it's not called in vanilla (it is in Forge, but that
        // shouldn't matter).
        setDragging(false);
        var child = getChildAt(x, y);
        if (child.isPresent() && child.get().mouseReleased(x, y, button)) return true;

        return super.mouseReleased(x, y, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderTooltip(graphics, mouseX, mouseY);
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
    public void setFocused(@Nullable GuiEventListener listener) {
        // Don't clear and re-focus if we're already focused.
        if (listener != getFocused()) super.setFocused(listener);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
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
                if (fileSize > uploadMaxSize || (size += fileSize) >= uploadMaxSize) {
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

        if (!toUpload.isEmpty()) UploadFileMessage.send(menu, toUpload, ClientNetworking::sendToServer);
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
        OptionScreen.show(minecraft(), title, message,
            List.of(OptionScreen.newButton(OK, b -> minecraft().setScreen(this))),
            () -> minecraft().setScreen(this)
        );
    }

    private Minecraft minecraft() {
        return Nullability.assertNonNull(minecraft);
    }
}
