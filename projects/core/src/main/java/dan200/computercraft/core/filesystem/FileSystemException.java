// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.filesystem;

import java.io.IOException;
import java.io.Serial;

import static dan200.computercraft.api.filesystem.MountConstants.ACCESS_DENIED;

public class FileSystemException extends Exception {
    @Serial
    private static final long serialVersionUID = -2500631644868104029L;

    FileSystemException(String message) {
        super(message);
    }

    FileSystemException(String path, String message) {
        this("/" + path + ": " + message);
    }

    public static FileSystemException of(IOException e) {
        return new FileSystemException(getMessage(e));
    }

    public static String getMessage(IOException e) {
        return e.getMessage() == null ? ACCESS_DENIED : e.getMessage();
    }
}
