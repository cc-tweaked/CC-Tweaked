/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import java.io.IOException;
import java.io.Serial;

public class FileSystemException extends Exception {
    @Serial
    private static final long serialVersionUID = -2500631644868104029L;

    FileSystemException(String s) {
        super(s);
    }

    public static FileSystemException of(IOException e) {
        return new FileSystemException(getMessage(e));
    }

    public static String getMessage(IOException e) {
        return e.getMessage() == null ? "Access denied" : e.getMessage();
    }
}
