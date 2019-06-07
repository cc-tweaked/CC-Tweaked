/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.api.filesystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;

/**
 * An {@link IOException} which occurred on a specific file.
 *
 * This may be thrown from a {@link IMount} or {@link IWritableMount} to give more information about a failure.
 */
public class FileOperationException extends IOException
{
    private static final long serialVersionUID = -8809108200853029849L;

    private String filename;

    public FileOperationException( @Nullable String filename, @Nonnull String message )
    {
        super( Objects.requireNonNull( message, "message cannot be null" ) );
        this.filename = filename;
    }

    public FileOperationException( String message )
    {
        super( Objects.requireNonNull( message, "message cannot be null" ) );
    }

    @Nullable
    public String getFilename()
    {
        return filename;
    }
}
