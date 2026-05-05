// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.request;

import com.google.errorprone.annotations.DoNotCall;
import dan200.computercraft.api.lua.ILuaCallback;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class HttpStreamReader {
    private static final int BUFFER_SIZE = 8192;

    private final HttpRequest request;
    private final HttpRequestHandler handler;
    private final boolean binary;

    private boolean isClosed = false;
    private ByteBuffer single = ByteBuffer.allocate(1);

    public HttpStreamReader(HttpRequest request, HttpRequestHandler handler) {
        this.request = request;
        this.handler = handler;
        this.binary = request.isBinary();
    }

    protected void checkOpen() throws LuaException {
        if (isClosed) throw new LuaException("attempt to use a closed connection");
    }

    @LuaFunction
    public final void close() throws LuaException {
        checkOpen();
        isClosed = true;
        handler.close();
    }

    @LuaFunction
    public final MethodResult read(Optional<Integer> countArg, Optional<Boolean> blockingArg) throws LuaException {
        checkOpen();

        if (binary && countArg.isEmpty()) {
            return new HttpContentSingleBytePoller(request.address).pollBody();
        }

        int count = countArg.orElse(1);
        boolean blocking = blockingArg.orElse(true);

        if (count == 0) return MethodResult.of("");
        if (count <= BUFFER_SIZE) {
            return new HttpContentSimpleBufferPoller(request.address, blocking, ByteBuffer.allocate(count)).pollBody();
        }

        return new HttpContentLargePoller(request.address, blocking, count).pollBody();
    }

    @LuaFunction
    public final MethodResult readAll() throws LuaException {
        checkOpen();

        return new HttpContentAllPoller(request.address, true).pollBody();
    }

    @LuaFunction
    public final MethodResult readLine(Optional<Boolean> withTrailingArg) throws LuaException {
        checkOpen();

        boolean withTrailing = withTrailingArg.orElse(false);
        return new HttpContentLinePoller(request.address, withTrailing).pollBody();
    }

    @DoNotCall
    @LuaFunction
    public final MethodResult seek(Optional<String> whence, Optional<Long> offset) throws LuaException {
        throw new LuaException("cannot seek on a streamed connection");
    }

    private static abstract class HttpContentPoller implements ILuaCallback {
        final MethodResult pull = MethodResult.pullEvent(HttpRequest.CONTENT_EVENT, this);
        private final String url;
        final boolean blocking;

        private HttpContentPoller(String url, boolean blocking) {
            this.url = url;
            this.blocking = blocking;
        }

        @Override
        public MethodResult resume(@Nullable Object[] args) throws LuaException {
            if (args.length < 2 || !Objects.equals(args[0], HttpRequest.CONTENT_EVENT) || !Objects.equals(args[1], url)) {
                return pull;
            }
            return this.pollBody();
        }

        abstract MethodResult pollBody() throws LuaException;
    }

    private final class HttpContentSingleBytePoller extends HttpContentPoller {
        private HttpContentSingleBytePoller(String url) {
            super(url, true);
        }

        @Override
        MethodResult pollBody() throws LuaException {
            checkOpen();

            single.clear();
            var read = handler.readBody(single);
            if (read == 0) return pull;
            if (read < 0) return MethodResult.of();
            return MethodResult.of(single.get(0) & 0xff);
        }
    }

    private final class HttpContentSimpleBufferPoller extends HttpContentPoller {
        private final ByteBuffer buffer;

        private HttpContentSimpleBufferPoller(String url, boolean blocking, ByteBuffer buffer) {
            super(url, blocking);
            this.buffer = buffer;
        }

        @Override
        MethodResult pollBody() throws LuaException {
            checkOpen();

            if (!buffer.hasRemaining()) return MethodResult.of(buffer.flip());
            var read = handler.readBody(buffer);
            if (read < 0) {
                buffer.flip();
                return MethodResult.of(buffer.hasRemaining() ? buffer : null);
            }
            if (read == 0) return blocking ? pull : MethodResult.of(buffer.flip());
            if (blocking && buffer.hasRemaining()) return pull;
            return MethodResult.of(buffer.flip());
        }
    }

    private static abstract class HttpContentPartsPoller extends HttpContentPoller {
        final List<ByteBuffer> parts = new ArrayList<>(4);
        int totalRead = 0;

        private HttpContentPartsPoller(String url, boolean blocking) {
            super(url, blocking);
        }

        byte[] joinParts() {
            var bytes = new byte[totalRead];
            var pos = 0;
            for (var part : parts) {
                var length = part.remaining();
                part.get(bytes, pos, length);
                pos += length;
            }
            assert pos == totalRead;
            return bytes;
        }
    }

    private final class HttpContentLargePoller extends HttpContentPartsPoller {
        private final int count;
        private @Nullable ByteBuffer buffer = null;

        private HttpContentLargePoller(String url, boolean blocking, int count) {
            super(url, blocking);
            this.count = count;
        }

        @Override
        MethodResult pollBody() throws LuaException {
            checkOpen();

            while (totalRead < count) {
                if (buffer == null) buffer = ByteBuffer.allocate(Math.min(BUFFER_SIZE, count - totalRead));
                var read = handler.readBody(buffer);
                if (read < 0) {
                    buffer.flip();
                    if (buffer.hasRemaining()) parts.add(buffer);
                    else if (parts.isEmpty()) return MethodResult.of();
                    break;
                }

                totalRead += read;
                if (read == 0) {
                    if (blocking) return pull;
                    parts.add(buffer.flip());
                    break;
                }

                if (!buffer.hasRemaining()) {
                    parts.add(buffer.flip());
                    buffer = null;
                }
            }
            return MethodResult.of(joinParts());
        }
    }

    private final class HttpContentAllPoller extends HttpContentPartsPoller {
        private ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        private HttpContentAllPoller(String url, boolean blocking) {
            super(url, blocking);
        }

        @Override
        MethodResult pollBody() throws LuaException {
            checkOpen();

            while (true) {
                var read = handler.readBody(buffer);
                if (read < 0) {
                    buffer.flip();
                    if (buffer.hasRemaining()) parts.add(buffer);
                    break;
                }

                totalRead += read;
                if (read == 0) {
                    if (blocking) return pull;
                    parts.add(buffer.flip());
                    break;
                }

                if (!buffer.hasRemaining()) {
                    parts.add(buffer.flip());
                    buffer = ByteBuffer.allocate(BUFFER_SIZE);
                }
            }
            return MethodResult.of(joinParts());
        }
    }

    private final class HttpContentLinePoller extends HttpContentPartsPoller {
        private static final byte SEP = '\n';
        private static final byte CR = '\r';

        private final boolean withTrailing;
        private ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        private HttpContentLinePoller(String url, boolean withTrailing) {
            super(url, true);
            this.withTrailing = withTrailing;
        }

        @Override
        MethodResult pollBody() throws LuaException {
            checkOpen();

            while (true) {
                var read = handler.readBodyUntil(buffer, SEP);
                if (read < 0) {
                    buffer.flip();
                    if (buffer.hasRemaining()) parts.add(buffer);
                    break;
                }

                totalRead += read;
                if (read == 0) {
                    return pull;
                }

                if (!buffer.hasRemaining()) {
                    parts.add(buffer.flip());
                    if (buffer.get(buffer.limit()) == SEP) {
                        if (!withTrailing) {
                            // trim LF
                            buffer.limit(buffer.limit() - 1);
                            totalRead--;
                            // trim CR
                            var buf = buffer;
                            if (buf.remaining() < 2) {
                                buf = parts.size() >= 2 ? parts.get(parts.size() - 2) : null;
                            }
                            if (buf != null && buf.get(buf.limit()) == CR) {
                                buf.limit(buf.limit() - 1);
                                totalRead--;
                            }
                        }
                        break;
                    }
                    buffer = ByteBuffer.allocate(BUFFER_SIZE);
                }
            }
            return MethodResult.of(joinParts());
        }
    }
}
