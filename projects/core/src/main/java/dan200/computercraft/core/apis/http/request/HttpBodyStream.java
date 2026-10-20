// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.request;

import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * A readable stream from an HTTP request.
 * <p>
 * This interface assumes a single consumer (i.e. {@link HttpStreamReader}). As such {@link #getReadable()} is safe
 * to use before calls to {@link #read(int)}, as contents will not be read by another thread.
 */
public interface HttpBodyStream {
    /**
     * Return whether the producer has stopped writing to the stream, so no more content will be written to it.
     *
     * @return Whether the stream has stopped producing content.
     */
    boolean isFinished();

    /**
     * Return the number of readable bytes in the stream.
     *
     * @return The number of readable bytes.
     */
    int getReadable();

    /**
     * Get the maximum number of bytes that can be read at once.
     * <p>
     * {@link #getReadable()} may be larger than
     *
     * @return The maximum number of bytes that can be read.
     */
    int getMaxReadable();

    /**
     * Mark that consumers are waiting for new content.
     */
    void markWaiting();

    /**
     * Read into the supplied buffer.
     *
     * @param buffer The buffer to read in to. {@link ByteBuffer#position()} will be advanced with the number of bytes
     *               read.
     */
    void read(ByteBuffer buffer);

    /**
     * Allocate and then read {@code count} bytes.
     *
     * @param count The number of bytes to read.
     * @return The bytes which have been read.
     */
    default ByteBuffer read(int count) {
        var buffer = ByteBuffer.allocateDirect(count);
        read(buffer);
        return buffer.flip();
    }

    /**
     * Read from the stream until a separator or end of file. The separator is included in the output buffer.
     * <p>
     * If the separator has not been reached, and bytes are still being received, this function returns {@code null},
     * indicating that the consumer should wait for more content to be received and try again.
     *
     * @param separator The target separator
     * @return The bytes which were read, or {@code null}.
     */
    @Nullable ByteBuffer readUntil(byte separator);
}
