// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.request;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.core.apis.HTTPAPI;
import dan200.computercraft.core.apis.handles.ReadHandle;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

/**
 * A http response for streaming requests.
 * <p>
 * This provides the same methods as a {@link HttpResponseHandle} (and thus a {@link ReadHandle file}), but with
 * additional support for asynchronous reading of the body.
 *
 * @cc.usage Make a streaming HTTP request and then print the response line-by-line.
 * <pre>{@code
 * local response = assert(http.get { url = "https://example.tweaked.cc/stream", streaming = true })
 * while true do
 *   local line = response.readLine()
 *   if not line then break end
 *   print(line)
 * end
 * response.close()
 * }</pre>
 * @cc.module http.StreamingResponse
 * @cc.since 1.121.0
 * @see HTTPAPI#request(IArguments)
 */
public final class HttpStreamReader {
    private final HttpBodyStream stream;
    private final String address;
    private final boolean binary;

    private boolean isClosed = false;
    private final ByteBuffer single = ByteBuffer.allocate(1);

    public HttpStreamReader(HttpBodyStream stream, String address, boolean binary) {
        this.stream = stream;
        this.address = address;
        this.binary = binary;
    }

    private void checkOpen() throws LuaException {
        if (isClosed) {
            throw new LuaException("attempt to use a closed connection");
        }
    }

    /**
     * Close this HTTP response, terminating the connection to the server.
     * <p>
     * Once a response has closed it may no longer be read from.
     *
     * @throws LuaException If the response has already been closed.
     */
    @LuaFunction
    public void close() throws LuaException {
        checkOpen();
        isClosed = true;
        stream.closeStream();
    }

    private abstract class ContentPoller implements ILuaCallback {
        private final MethodResult pull = MethodResult.pullEvent(HttpRequest.CONTENT_EVENT, this);

        @Override
        public MethodResult resume(@Nullable Object[] args) throws LuaException {
            if (args.length < 2 || !Objects.equals(args[0], HttpRequest.CONTENT_EVENT) || !Objects.equals(args[1], address)) {
                return pull;
            }

            checkOpen();
            return pollBody();
        }

        MethodResult awaitContent() {
            stream.markWaiting();
            return pull;
        }

        abstract MethodResult pollBody() throws LuaException;
    }

    /**
     * Read a number of bytes from the response's body.
     *
     * @param countArg    The number of bytes to read. This may be 0 to determine we are at the end of the request. When
     *                    absent, a single byte will be read.
     * @param blockingArg When {@code false}, this function will read at most {@code count} bytes and return immediately,
     *                    rather than waiting for more content to be available.
     * @return The read bytes.
     * @throws LuaException When trying to read a negative number of bytes.
     * @throws LuaException When trying to read too many bytes.
     * @throws LuaException If the response has been closed.
     * @cc.treturn [1] nil If we are at the end of the response's body.
     * @cc.treturn [2] number The value of the byte read. This is returned if the request is opened in binary mode
     * and {@code count} is absent
     * @cc.treturn [3] string The bytes read as a string. This is returned when the {@code count} is given.
     */
    @LuaFunction
    public MethodResult read(Optional<Integer> countArg, Optional<Boolean> blockingArg) throws LuaException {
        checkOpen();

        if (binary && countArg.isEmpty()) {
            // TODO: FIXME: This doesn't handle blocking and/or empty streams.
            return new ContentSingleBytePoller().pollBody();
        }

        int count = countArg.orElse(1);
        boolean blocking = blockingArg.orElse(true);

        if (count < 0) throw new LuaException("Cannot read a negative number of bytes");
        if (count > stream.getMaxReadable()) {
            throw new LuaException("Cannot read more than " + stream.getMaxReadable() + " bytes");
        }

        // Fast path for non-blocking reads or if the stream has sufficient contents/is readable.
        var finished = stream.isFinished();
        var readable = stream.getReadable();
        if (finished && readable == 0) return MethodResult.of();
        if (count == 0) return MethodResult.of("");
        if (!blocking || finished || readable >= count) {
            return MethodResult.of(stream.read(Math.min(readable, count)).asReadOnlyBuffer());
        }

        return new ReadPoller(count).pollBody();
    }

    private final class ContentSingleBytePoller extends ContentPoller {
        @Override
        MethodResult pollBody() {
            if (stream.getReadable() > 0) {
                single.clear();
                stream.read(single);
                return MethodResult.of(single.get(0) & 0xff);
            }

            if (stream.isFinished()) return MethodResult.of(false);

            return awaitContent();
        }
    }

    private final class ReadPoller extends ContentPoller {
        private final int count;

        private ReadPoller(int count) {
            this.count = count;
        }

        @Override
        MethodResult pollBody() {
            var finished = stream.isFinished();
            var readable = stream.getReadable();
            if (finished && readable == 0) return MethodResult.of();
            if (finished || readable >= count) {
                return MethodResult.of(stream.read(Math.min(readable, count)).asReadOnlyBuffer());
            }

            return awaitContent();
        }
    }

    /**
     * Read the remainder of the response's body.
     *
     * @return The remaining contents of the body, or {@code null} in the event of an error.
     * @throws LuaException If the response has been closed.
     * @throws LuaException If the body is too large to be read in one go.
     * @cc.treturn string The remaining response body.
     */
    @LuaFunction
    public MethodResult readAll() throws LuaException {
        checkOpen();
        return new ReadAllPoller().pollBody();
    }

    private final class ReadAllPoller extends ContentPoller {
        @Override
        MethodResult pollBody() throws LuaException {
            var readable = stream.getReadable();
            if (readable > stream.getMaxReadable()) throw new LuaException("Body is too large to read at once");
            if (stream.isFinished()) return MethodResult.of(stream.read(readable).asReadOnlyBuffer());
            return awaitContent();
        }
    }

    /**
     * Read a line from the response's body.
     *
     * @param withTrailingArg Whether to include the newline characters with the returned string. Defaults to {@code false}.
     * @return The read string.
     * @throws LuaException If the response has been closed.
     * @throws LuaException If the line is too large to be read in one go.
     * @cc.treturn string|nil The read line or {@code nil} if at the end of the response.
     */
    @LuaFunction
    public MethodResult readLine(Optional<Boolean> withTrailingArg) throws LuaException {
        checkOpen();

        boolean withTrailing = withTrailingArg.orElse(false);
        return new ReadLinePoller(withTrailing).pollBody();
    }

    private final class ReadLinePoller extends ContentPoller {
        private static final byte LF = '\n';
        private static final byte CR = '\r';

        private final boolean withTrailing;

        private ReadLinePoller(boolean withTrailing) {
            this.withTrailing = withTrailing;
        }

        @Override
        MethodResult pollBody() throws LuaException {
            // If at the end of the stream, then return nil.
            var readable = stream.getReadable();
            if (stream.isFinished() && readable == 0) return MethodResult.of();

            var result = stream.readUntil(LF);
            if (result != null) {
                if (!withTrailing) trimLineFeed(result);
                return MethodResult.of(result.asReadOnlyBuffer());
            }

            // If the response is larger than the buffer size, then it's impossible for us to ever read a line.
            if (readable > stream.getMaxReadable()) throw new LuaException("Line is too large to read at once");

            return awaitContent();
        }

        private static void trimLineFeed(ByteBuffer buffer) {
            if (buffer.hasRemaining() && buffer.get(buffer.limit() - 1) == LF) {
                buffer.limit(buffer.limit() - 1);
                if (buffer.hasRemaining() && buffer.get(buffer.limit() - 1) == CR) {
                    buffer.limit(buffer.limit() - 1);
                }
            }
        }
    }
}
