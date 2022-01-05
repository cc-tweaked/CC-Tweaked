/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import java.io.Closeable;
import java.io.IOException;

/**
 * A {@link Closeable} which knows when it has been closed.
 *
 * This is a quick (though racey) way of providing more friendly (and more similar to Lua)
 * error messages to the user.
 */
public interface TrackingCloseable extends Closeable
{
    boolean isOpen();

    class Impl implements TrackingCloseable
    {
        private final Closeable object;
        private boolean isOpen = true;

        public Impl( Closeable object )
        {
            this.object = object;
        }

        @Override
        public boolean isOpen()
        {
            return isOpen;
        }

        @Override
        public void close() throws IOException
        {
            isOpen = false;
            object.close();
        }
    }
}
