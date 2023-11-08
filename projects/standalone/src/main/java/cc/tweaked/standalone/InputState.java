// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.standalone;

import dan200.computercraft.core.apis.handles.ArrayByteChannel;
import dan200.computercraft.core.apis.transfer.TransferredFile;
import dan200.computercraft.core.apis.transfer.TransferredFiles;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.util.StringUtil;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Manages the input to a computer. This receives GLFW events (i.e. {@link GLFWKeyCallbackI} and queues them to be
 * run on the computer.
 */
public class InputState {
    private static final Logger LOG = LoggerFactory.getLogger(InputState.class);

    private static final float TERMINATE_TIME = 0.5f;
    private static final float KEY_SUPPRESS_DELAY = 0.2f;

    private final Computer computer;
    private final BitSet keysDown = new BitSet(256);

    private float terminateTimer = -1;
    private float rebootTimer = -1;
    private float shutdownTimer = -1;

    private int lastMouseButton = -1;
    private int lastMouseX = -1;
    private int lastMouseY = -1;

    public InputState(Computer computer) {
        this.computer = computer;
    }

    public void onCharEvent(int codepoint) {
        if (codepoint >= 32 && codepoint <= 126 || codepoint >= 160 && codepoint <= 255) {
            // Queue the char event for any printable chars in byte range
            computer.queueEvent("char", new Object[]{ Character.toString(codepoint) });
        }
    }

    public void onKeyEvent(long window, int key, int action, int modifiers) {
        switch (action) {
            case GLFW.GLFW_PRESS, GLFW.GLFW_REPEAT -> keyPressed(window, key, modifiers);
            case GLFW.GLFW_RELEASE -> keyReleased(key);
        }
    }

    private void keyPressed(long window, int key, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) return;

        if (key == GLFW.GLFW_KEY_V && modifiers == GLFW.GLFW_MOD_CONTROL) {
            var string = GLFW.glfwGetClipboardString(window);
            if (string != null) {
                var clipboard = StringUtil.normaliseClipboardString(string);
                if (!clipboard.isEmpty()) computer.queueEvent("paste", new Object[]{ clipboard });
            }
            return;
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            switch (key) {
                case GLFW.GLFW_KEY_T -> {
                    if (terminateTimer < 0) terminateTimer = 0;
                }
                case GLFW.GLFW_KEY_S -> {
                    if (shutdownTimer < 0) shutdownTimer = 0;
                }
                case GLFW.GLFW_KEY_R -> {
                    if (rebootTimer < 0) rebootTimer = 0;
                }
            }
        }

        if (key >= 0 && terminateTimer < KEY_SUPPRESS_DELAY && rebootTimer < KEY_SUPPRESS_DELAY && shutdownTimer < KEY_SUPPRESS_DELAY) {
            // Queue the "key" event and add to the down set
            var repeat = keysDown.get(key);
            keysDown.set(key);
            computer.queueEvent("key", new Object[]{ key, repeat });
        }
    }

    private void keyReleased(int key) {
        // Queue the "key_up" event and remove from the down set
        if (key >= 0 && keysDown.get(key)) {
            keysDown.set(key, false);
            computer.queueEvent("key_up", new Object[]{ key });
        }

        switch (key) {
            case GLFW.GLFW_KEY_T -> terminateTimer = -1;
            case GLFW.GLFW_KEY_R -> rebootTimer = -1;
            case GLFW.GLFW_KEY_S -> shutdownTimer = -1;
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL ->
                terminateTimer = rebootTimer = shutdownTimer = -1;
        }
    }

    public void onMouseClick(int button, int action) {
        switch (action) {
            case GLFW.GLFW_PRESS -> {
                computer.queueEvent("mouse_click", new Object[]{ button + 1, lastMouseX + 1, lastMouseY + 1 });
                lastMouseButton = button;
            }
            case GLFW.GLFW_RELEASE -> {
                if (button == lastMouseButton) {
                    computer.queueEvent("mouse_click", new Object[]{ button + 1, lastMouseX + 1, lastMouseY + 1 });
                    lastMouseButton = -1;
                }
            }
        }
    }

    public void onMouseMove(int mouseX, int mouseY) {
        if (mouseX == lastMouseX && mouseY == lastMouseY) return;

        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (lastMouseButton != -1) {
            computer.queueEvent("mouse_drag", new Object[]{ lastMouseButton + 1, mouseX + 1, mouseY + 1 });
        }
    }

    public void onMouseScroll(double yOffset) {
        if (yOffset != 0) {
            computer.queueEvent("mouse_scroll", new Object[]{ yOffset < 0 ? 1 : -1, lastMouseX + 1, lastMouseY + 1 });
        }
    }

    public void onFileDrop(int count, long names) {
        var paths = new Path[count];
        for (var i = 0; i < count; ++i) paths[i] = Paths.get(GLFWDropCallback.getName(names, i));

        List<TransferredFile> files = new ArrayList<>();
        for (var path : paths) {
            if (!Files.isRegularFile(path)) continue;

            byte[] contents;
            try {
                contents = Files.readAllBytes(path);
            } catch (IOException e) {
                LOG.error("Failed to read {}", path, e);
                continue;
            }

            files.add(new TransferredFile(path.getFileName().toString(), new ArrayByteChannel(contents)));
        }

        if (!files.isEmpty()) computer.queueEvent(TransferredFiles.EVENT, new Object[]{ new TransferredFiles(files) });
    }

    public void update() {
        if (terminateTimer >= 0 && terminateTimer < TERMINATE_TIME && (terminateTimer += 0.05f) > TERMINATE_TIME) {
            computer.queueEvent("terminate", null);
        }

        if (shutdownTimer >= 0 && shutdownTimer < TERMINATE_TIME && (shutdownTimer += 0.05f) > TERMINATE_TIME) {
            computer.shutdown();
        }

        if (rebootTimer >= 0 && rebootTimer < TERMINATE_TIME && (rebootTimer += 0.05f) > TERMINATE_TIME) {
            if (computer.isOn()) {
                computer.reboot();
            } else {
                computer.turnOn();
            }
        }
    }
}
