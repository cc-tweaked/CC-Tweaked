// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.request;

import dan200.computercraft.api.lua.ILuaCallback;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

public final class HttpStreamReader {
    private final HttpRequest request;
    private final HttpBodyStream stream;
    private final boolean binary;

    private boolean isClosed = false;
    private final ByteBuffer single = ByteBuffer.allocate(1);

    public HttpStreamReader(HttpRequest request, HttpBodyStream stream) {
        this.request = request;
        this.stream = stream;
        this.binary = request.isBinary();
    }

    private void checkOpen() throws LuaException {
        if (isClosed) {
            throw new LuaException("attempt to use a closed connection");
        }
    }

    @LuaFunction
    public void close() throws LuaException {
        checkOpen();
        isClosed = true;
        request.partialClosed();
    }

    private abstract class ContentPoller implements ILuaCallback {
        private final MethodResult pull = MethodResult.pullEvent(HttpRequest.CONTENT_EVENT, this);

        @Override
        public MethodResult resume(@Nullable Object[] args) throws LuaException {
            if (args.length < 2 || !Objects.equals(args[0], HttpRequest.CONTENT_EVENT) || !Objects.equals(args[1], request.address)) {
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

        if (count == 0) return MethodResult.of("");

        // Fast path for non-blocking reads or if the stream has sufficient contents/is readable.
        var finished = stream.isFinished();
        var readable = stream.getReadable();
        if (finished && readable == 0) return MethodResult.of();
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
        MethodResult pollBody() {
            var result = stream.readUntil(LF);
            if (result == null) return awaitContent();

            // Trim CR and LF if needed.
            if (!withTrailing && result.hasRemaining() && result.get(result.limit() - 1) == LF) {
                result.limit(result.limit() - 1);
                if (result.hasRemaining() && result.get(result.limit() - 1) == CR) {
                    result.limit(result.limit() - 1);
                }
            }

            return MethodResult.of(result.asReadOnlyBuffer());
        }
    }
}
