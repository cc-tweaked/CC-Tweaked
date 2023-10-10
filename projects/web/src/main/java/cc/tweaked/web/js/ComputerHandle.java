// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.js;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.ArrayBuffer;

import javax.annotation.Nullable;

/**
 * A Javascript-facing interface for controlling computers.
 */
public interface ComputerHandle extends JSObject {
    /**
     * Queue an event on the computer.
     *
     * @param event The name of the event.
     * @param args  The arguments for this event.
     */
    void event(String event, @Nullable JSObject[] args);

    /**
     * Shut the computer down.
     */
    void shutdown();

    /**
     * Turn the computer on.
     */
    void turnOn();

    /**
     * Reboot the computer.
     */
    void reboot();

    /**
     * Dispose of this computer, marking it as no longer running.
     */
    void dispose();

    /**
     * Transfer some files to this computer.
     *
     * @param files A list of files and their contents.
     */
    void transferFiles(FileContents[] files);

    /**
     * Set a peripheral on a particular side.
     *
     * @param side The side to set the peripheral on.
     * @param kind The kind of peripheral. For now, can only be "speaker".
     */
    void setPeripheral(String side, @Nullable String kind);

    /**
     * Add a file to this computer's filesystem.
     *
     * @param path     The path of the file.
     * @param contents The contents of the file, either a {@link JSString} or {@link ArrayBuffer}.
     */
    void addFile(String path, JSObject contents);

    /**
     * A file to transfer to the computer.
     */
    interface FileContents extends JSObject {
        @JSProperty
        String getName();

        @JSProperty
        ArrayBuffer getContents();
    }
}
