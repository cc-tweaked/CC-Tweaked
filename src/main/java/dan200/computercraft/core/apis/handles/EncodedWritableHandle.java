// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.handles;

import dan200.computer.core.IMountedFileNormal;
import dan200.computercraft.api.lua.Coerced;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.apis.FSAPI;

import java.io.IOException;

/**
 * A file handle opened by {@link FSAPI#open} using the {@code "w"} or {@code "a"} modes.
 *
 * @cc.module fs.WriteHandle
 */
public class EncodedWritableHandle extends HandleGeneric {
    private final IMountedFileNormal writer;

    public EncodedWritableHandle(IMountedFileNormal writer) {
        super(writer);
        this.writer = writer;
    }

    /**
     * Write a string of characters to the file.
     *
     * @param textA The text to write to the file.
     * @throws LuaException If the file has been closed.
     */
    @LuaFunction
    public final void write(Coerced<String> textA) throws LuaException {
        checkOpen();
        String text = textA.value();
        try {
            writer.write(text, 0, text.length(), false);
        } catch (IOException e) {
            throw new LuaException(e.getMessage());
        }
    }

    /**
     * Write a string of characters to the file, following them with a new line character.
     *
     * @param textA The text to write to the file.
     * @throws LuaException If the file has been closed.
     */
    @LuaFunction
    public final void writeLine(Coerced<String> textA) throws LuaException {
        checkOpen();
        String text = textA.value();
        try {
            writer.write(text, 0, text.length(), true);
        } catch (IOException e) {
            throw new LuaException(e.getMessage());
        }
    }

    /**
     * Save the current file without closing it.
     *
     * @throws LuaException If the file has been closed.
     */
    @LuaFunction
    public final void flush() throws LuaException {
        checkOpen();
        try {
            writer.flush();
        } catch (IOException e) {
            throw new LuaException(e.getMessage());
        }
    }
}
