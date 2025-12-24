// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.standalone;

import dan200.computercraft.core.apis.handles.ArrayByteChannel;
import dan200.computercraft.core.apis.transfer.TransferredFile;
import dan200.computercraft.core.apis.transfer.TransferredFiles;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.input.EventComputerInput;
import dan200.computercraft.core.input.UserComputerInput;
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
    private final UserComputerInput input;

    private float terminateTimer = -1;
    private float rebootTimer = -1;
    private float shutdownTimer = -1;

    public InputState(Computer computer) {
        this.computer = computer;
        this.input = new UserComputerInput(new EventComputerInput(computer), computer.getEnvironment().getTerminal());
    }

    public void onCharEvent(int codepoint) {
        input.codepointTyped(codepoint);
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
            if (string != null) input.paste(string);
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
            input.keyDown(key);
        }
    }

    private void keyReleased(int key) {
        input.keyUp(key);

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
            case GLFW.GLFW_PRESS -> input.mouseClick(button + 1);
            case GLFW.GLFW_RELEASE -> input.mouseUp(button + 1);
        }
    }

    public void onMouseMove(int mouseX, int mouseY) {
        input.mouseMove(mouseX + 1, mouseY + 1);
    }

    public void onMouseScroll(double yOffset) {
        if (yOffset != 0) input.mouseScroll(yOffset < 0 ? 1 : -1);
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
