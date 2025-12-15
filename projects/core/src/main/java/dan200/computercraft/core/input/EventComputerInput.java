// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.input;

import dan200.computercraft.core.computer.Computer;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * A {@link ComputerInput} that queues events on the computer.
 */
public final class EventComputerInput implements ComputerInput {
    private final QueueEvent receiver;

    public EventComputerInput(QueueEvent receiver) {
        this.receiver = receiver;
    }

    public EventComputerInput(Computer computer) {
        this(computer::queueEvent);
    }

    @Override
    public void keyDown(int key, boolean repeat) {
        receiver.queueEvent("key", new Object[]{ key, repeat });
    }

    @Override
    public void keyUp(int key) {
        receiver.queueEvent("key_up", new Object[]{ key });
    }

    @Override
    public void charTyped(byte chr) {
        receiver.queueEvent("char", new Object[]{ new byte[]{ chr } });
    }

    @Override
    public void paste(ByteBuffer contents) {
        receiver.queueEvent("paste", new Object[]{ contents });
    }

    @Override
    public void mouseClick(int button, int x, int y) {
        receiver.queueEvent("mouse_click", new Object[]{ button, x, y });
    }

    @Override
    public void mouseUp(int button, int x, int y) {
        receiver.queueEvent("mouse_up", new Object[]{ button, x, y });
    }

    @Override
    public void mouseDrag(int button, int x, int y) {
        receiver.queueEvent("mouse_drag", new Object[]{ button, x, y });
    }

    @Override
    public void mouseScroll(int direction, int x, int y) {
        receiver.queueEvent("mouse_scroll", new Object[]{ direction, x, y });
    }

    /**
     * A function to queue events.
     */
    @FunctionalInterface
    public interface QueueEvent {
        void queueEvent(String event, @Nullable Object @Nullable [] arguments);
    }
}
