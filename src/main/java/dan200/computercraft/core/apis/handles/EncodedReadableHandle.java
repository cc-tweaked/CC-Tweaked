// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.handles;

import dan200.computer.core.IMountedFileNormal;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.apis.FSAPI;

import java.io.IOException;
import java.util.Optional;

/**
 * A file handle opened with {@link FSAPI#open(String, String)} with the {@code "r"}
 * mode.
 *
 * @cc.module fs.ReadHandle
 */
public class EncodedReadableHandle extends HandleGeneric {
    private final IMountedFileNormal reader;

    public EncodedReadableHandle(IMountedFileNormal reader) {
        super(reader);
        this.reader = reader;
    }

    /**
     * Read a line from the file.
     *
     * @param withTrailingArg Whether to include the newline characters with the returned string. Defaults to {@code false}.
     * @return The read string.
     * @throws LuaException If the file has been closed.
     * @cc.treturn string|nil The read line or {@code nil} if at the end of the file.
     * @cc.changed 1.81.0 Added option to return trailing newline.
     */
    @LuaFunction
    public final Object[] readLine(Optional<Boolean> withTrailingArg) throws LuaException {
        checkOpen();
        boolean withTrailing = withTrailingArg.orElse(false);
        try {
            String line = reader.readLine();
            if (line != null) {
                // While this is technically inaccurate, it's better than nothing
                if (withTrailing) line += "\n";
                return new Object[]{ line };
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Read the remainder of the file.
     *
     * @return The file, or {@code null} if at the end of it.
     * @throws LuaException If the file has been closed.
     * @cc.treturn nil|string The remaining contents of the file, or {@code nil} if we are at the end.
     */
    @LuaFunction
    public final Object[] readAll() throws LuaException {
        checkOpen();
        try {
            StringBuilder result = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                result.append(line);
                line = reader.readLine();
                if (line != null) {
                    result.append("\n");
                }
            }
            return new Object[]{ result.toString() };
        } catch (IOException e) {
            return null;
        }
    }
}
