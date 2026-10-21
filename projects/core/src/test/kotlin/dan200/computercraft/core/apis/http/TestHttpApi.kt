// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http

import dan200.computercraft.api.lua.*
import dan200.computercraft.core.apis.HTTPAPI
import dan200.computercraft.core.apis.IAPIEnvironment
import dan200.computercraft.core.apis.handles.ReadHandle
import dan200.computercraft.core.apis.http.TestHttpApi.Companion.INSTANT_DURATION
import dan200.computercraft.core.apis.http.TestHttpApi.Companion.LIMITED_DURATION
import dan200.computercraft.core.apis.http.options.Action
import dan200.computercraft.core.apis.http.options.AddressRule
import dan200.computercraft.core.apis.http.request.HttpResponseHandle
import dan200.computercraft.core.apis.http.request.HttpStreamReader
import dan200.computercraft.core.apis.http.websocket.WebsocketHandle
import dan200.computercraft.test.core.computer.LuaTaskRunner
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.LastHttpContent
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import java.lang.ref.Reference
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.random.RandomGenerator
import java.util.random.RandomGeneratorFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class TestHttpApi {
    /** Create a [HTTPAPI] which is only permitted to access the current server. */
    private fun createHttpApi(environment: IAPIEnvironment, port: Int, config: NettyHttp.Config = NettyHttp.DEFAULT_CONFIG): HTTPAPI {
        val rules = listOf(
            AddressRule.parse("127.0.0.1", OptionalInt.of(port), Action.ALLOW.toPartial()),
        )
        return HTTPAPI(environment, NettyHttp(config.withAddressRules(rules)).create(environment))
    }

    /** Create a custom [NettyHttp.Config]. */
    private fun createHttpConfig(
        uploadBandwidth: Int = NettyHttp.DEFAULT_CONFIG.uploadBandwidth,
        downloadBandwidth: Int = NettyHttp.DEFAULT_CONFIG.downloadBandwidth,
    ) = NettyHttp.Config(
        NettyHttp.DEFAULT_CONFIG.enabled,
        NettyHttp.DEFAULT_CONFIG.addressRules,
        NettyHttp.DEFAULT_CONFIG.maxRequests,
        NettyHttp.DEFAULT_CONFIG.maxWebsockets,
        downloadBandwidth,
        uploadBandwidth,
        NettyHttp.DEFAULT_CONFIG.proxy,
    )

    /**
     * Start an HTTP server, then run a test within a [LuaTaskRunner].
     *
     * This test runs with a longer timeout, as these tests are a little more timing-sensitive than most.
     */
    private fun runWithServer(closeWebsocket: Boolean = true, timeout: Duration = 10.seconds, fn: suspend LuaTaskRunner.(HttpServer, scope: CoroutineScope) -> Unit) {
        HttpServer.runServer(closeWebsocket = closeWebsocket) { server -> LuaTaskRunner.runTest(timeout) { scope -> fn(server, scope) } }
    }

    /** Assert the result of a `read()` function returns the given string. */
    private suspend fun LuaTaskRunner.assertRead(result: MethodResult, expected: String): Unit =
        assertRead(result.await(), expected)

    /** Assert the result of a `read()` function returns the given string. */
    private fun assertRead(result: Array<out Any?>?, expected: String) {
        assertThat(result, array(anyOf(isA(ByteBuffer::class.java), isA(ByteArray::class.java))))

        val contents = when (val buffer = result!![0]) {
            is ByteBuffer -> buffer
            is ByteArray -> ByteBuffer.wrap(buffer)
            else -> throw AssertionError("Must be a ByteBuffer or byte[]")
        }
        assertEquals(expected, StandardCharsets.UTF_8.decode(contents).toString())
    }

    @Test
    fun `Connects to a HTTP server`() {
        runWithServer { server, _ ->
            val url = "http://127.0.0.1:${server.port}"
            val httpApi = addApi(createHttpApi(environment, server.port))
            assertThat("http.request succeeded", httpApi.request(ObjectArguments(url)), array(equalTo(true)))

            val result = pullEvent("http_success")
            assertThat(result, array(equalTo("http_success"), equalTo(url), isA(HttpResponseHandle::class.java)))

            val handle = result[2] as HttpResponseHandle
            val reader = handle.extra.iterator().next() as ReadHandle
            assertRead(reader.readAll(), "Hello, world!")
        }
    }

    @Test
    fun `HTTP failure on 404`() {
        runWithServer { server, _ ->
            val url = "http://127.0.0.1:${server.port}/missing"
            val httpApi = addApi(createHttpApi(environment, server.port))
            assertThat("http.request succeeded", httpApi.request(ObjectArguments(url)), array(equalTo(true)))

            val result = pullEvent("http_failure")
            assertThat(result, array(equalTo("http_failure"), equalTo(url), equalTo("Not Found"), isA(HttpResponseHandle::class.java)))

            val handle = result[3] as HttpResponseHandle
            val reader = handle.extra.iterator().next() as ReadHandle
            assertRead(reader.readAll(), "404 Not Found")
        }
    }

    @Test
    fun `Handles connection error`() {
        runWithServer { server, _ ->
            val url = "https://127.0.0.1:${server.port}/missing"
            val httpApi = addApi(createHttpApi(environment, server.port))
            assertThat("http.request succeeded", httpApi.request(ObjectArguments(url)), array(equalTo(true)))

            val result = pullEvent("http_failure")
            assertThat(result, array(equalTo("http_failure"), equalTo(url), equalTo("Could not connect")))
        }
    }

    @Nested
    inner class Streaming {
        private suspend fun LuaTaskRunner.streaming(server: HttpServer, path: String, config: NettyHttp.Config = NettyHttp.DEFAULT_CONFIG): Triple<String, HttpResponseHandle, HttpStreamReader> {
            val url = "http://127.0.0.1:${server.port}$path"
            val httpApi = addApi(createHttpApi(environment, server.port, config))

            val request = httpApi.request(ObjectArguments(mapOf("url" to url, "streaming" to true)))
            assertThat("http.request succeeded", request, array(equalTo(true)))

            val result = pullEvent("http_success")
            assertThat(result, array(equalTo("http_success"), equalTo(url), isA(HttpResponseHandle::class.java)))

            val handle = result[2] as HttpResponseHandle
            val reader = handle.extra.single() as HttpStreamReader
            return Triple(url, handle, reader)
        }

        @Test
        fun `Can stream content`() {
            runWithServer { server, _ ->
                val (_, response, reader) = streaming(server, "/stream")
                assertRead(
                    reader.read(Optional.of(70), Optional.empty()),
                    "Hello, world!\n".repeat(5),
                )

                reader.close()
                Reference.reachabilityFence(response)
            }
        }

        /**
         * Test if `read()`-like functions are interrupted and abort when the remote socket closes cleanly.
         */
        @Test
        fun `read() aborts on clean exit`() {
            runWithServer { server, _ ->
                val (_, response, reader) = streaming(server, "/stream?limit=1")
                assertRead(reader.readAll(), "Hello, world!\n")
                reader.close()
                Reference.reachabilityFence(response)
            }
        }

        /**
         * Test if `read()`-like functions are interrupted and abort when the remote socket dies.
         *
         * In practice, this triggers the same codepath as the above test: [io.netty.handler.codec.ByteToMessageDecoder]
         * queues [LastHttpContent.EMPTY_LAST_CONTENT] on channel disconnect, so we always close the connection there.
         */
        @Test
        fun `read() aborts on abnormal exit`() {
            runWithServer { server, scope ->
                val (_, response, reader) = streaming(server, "/stream?limit=1&close=false")
                val readAll = scope.async { reader.readAll().await() }
                assertNull(withTimeoutOrNull(500.milliseconds) { readAll.await() })
                server.stop()
                assertRead(readAll.await(), "Hello, world!\n")

                reader.close()
                Reference.reachabilityFence(response)
            }
        }

        /**
         * Test if `read()`-like functions are interrupted and abort when the remote socket dies using chunked encoding.
         *
         * If the remote socket is using chunked encoding, Netty does *not* enqueue a
         * [LastHttpContent.EMPTY_LAST_CONTENT], so we wake up the readers in `channelInactive`.
         */
        @Test
        fun `read() aborts on abnormal exit (chunked)`() {
            runWithServer { server, scope ->
                val (_, response, reader) = streaming(server, "/stream?limit=1&close=false&chunked=true")
                val readAll = scope.async { reader.readAll().await() }
                assertNull(withTimeoutOrNull(500.milliseconds) { readAll.await() })
                server.stop()
                assertRead(readAll.await(), "Hello, world!\n")

                reader.close()
                Reference.reachabilityFence(response)
            }
        }

        @Test
        fun `Read line`() {
            runWithServer { server, _ ->
                val (_, response, reader) = streaming(server, "/stream")
                assertRead(reader.readLine(Optional.empty()), "Hello, world!")
                assertRead(reader.read(Optional.of(5), Optional.empty()), "Hello")
                assertRead(reader.readLine(Optional.of(true)), ", world!\n")
                reader.close()
                Reference.reachabilityFence(response)
            }
        }

        @Test
        fun `Read line waits for content`() {
            runWithServer { server, _ ->
                val (_, response, reader) = streaming(server, "/stream?delay=300")
                assertRead(reader.readLine(Optional.empty()), "Hello, world!")
                val read = reader.readLine(Optional.empty())
                assertThat(read.callback, notNullValue())
                assertThat(read.result, array(equalTo("http_content")))
                assertRead(read, "Hello, world!")

                reader.close()
                Reference.reachabilityFence(response)
            }
        }

        @Test
        fun `Read line returns remaining text on end`() {
            runWithServer { server, _ ->
                val (_, response, reader) = streaming(server, "/stream?limit=1&lines=false")
                assertRead(reader.readLine(Optional.empty()), "Hello, world!")
                assertRead(reader.readLine(Optional.empty()), "")
                reader.close()
                Reference.reachabilityFence(response)
            }
        }

        @Test
        fun `Obeys bandwidth limits`() {
            runWithServer { server, _ ->
                val (_, response, reader) = streaming(server, "/stream", createHttpConfig(downloadBandwidth = BANDWIDTH_MESSAGE_SIZE * BANDWIDTH_SCALE))

                val start = System.nanoTime()
                val deadline = start + TimeUnit.SECONDS.toNanos(2)
                var read = 0
                var now: Long
                do {
                    now = System.nanoTime()
                    reader.read(Optional.of(BANDWIDTH_MESSAGE_SIZE), Optional.empty()).await()
                    read += BANDWIDTH_MESSAGE_SIZE
                } while (now <= deadline)

                reader.close()
                Reference.reachabilityFence(response)

                val total = (now - start).nanoseconds
                val bytesPerSecond = read / (total.inWholeNanoseconds * 1e-9)
                assertThat("Bytes/second is less than twice bandwidth", bytesPerSecond, lessThan(BANDWIDTH_MESSAGE_SIZE * BANDWIDTH_SCALE * 2.0))
            }
        }
    }

    suspend fun LuaTaskRunner.websocket(server: HttpServer, httpApi: HTTPAPI): Pair<String, WebsocketHandle> {
        val url = "ws://127.0.0.1:${server.port}/ws"
        assertThat("http.websocket succeeded", httpApi.websocket(ObjectArguments(url)), array(equalTo(true)))

        val connectEvent = pullEvent()
        assertThat(connectEvent, array(equalTo("websocket_success"), equalTo(url), isA(WebsocketHandle::class.java)))

        return Pair(url, connectEvent[2] as WebsocketHandle)
    }

    @Test
    fun `Connects to websocket`() {
        runWithServer { server, _ ->
            val httpApi = addApi(createHttpApi(environment, server.port))
            val (url, websocket) = websocket(server, httpApi)

            websocket.send(Coerced(LuaValues.encode("Hello")), Optional.of(false))

            val message = websocket.receive(Optional.empty()).await()
            assertThat("Received a return message", message, array(equalTo("HELLO".toByteArray()), equalTo(false)))

            websocket.close()

            val closeEvent = pullEvent()
            assertThat("Received a close event", closeEvent, array(equalTo("websocket_closed"), equalTo(url), nullValue(), equalTo(1000)))

            assertFalse(server.lastRequest.headers.contains(HttpHeaderNames.ORIGIN), "HTTP Headers should not contain Origin")

            Reference.reachabilityFence(websocket)
        }
    }

    @Test
    fun `Errors if too many websocket messages are sent`() {
        runWithServer { server, _ ->
            val httpApi = addApi(createHttpApi(environment, server.port))
            val (_, websocket) = websocket(server, httpApi)

            val error = assertThrows<LuaException> {
                for (i in 0 until 10_000) {
                    websocket.send(Coerced(LuaValues.encode("Hello")), Optional.of(false))
                }
            }

            websocket.close()

            assertThat(error.message, equalTo("Too many ongoing websocket messages"))
        }
    }

    @Test
    fun `Closes if a websocket message is too large`() {
        runWithServer { server, _ ->
            val httpApi = addApi(createHttpApi(environment, server.port))
            val (url, websocket) = websocket(server, httpApi)

            val out = ByteArray(AddressRule.WEBSOCKET_MESSAGE + 1)
            Random(0xDEADBEEF).nextBytes(out)
            server.broadcast(BinaryWebSocketFrame(Unpooled.wrappedBuffer(out)))

            val closeEvent = pullEvent()
            assertThat(closeEvent, array(equalTo("websocket_closed"), equalTo(url), equalTo("Received a too-large message"), nullValue()))

            Reference.reachabilityFence(websocket)
        }
    }

    @Test
    fun `Queues an event when the socket is externally closed`() {
        runWithServer { server, _ ->
            val httpApi = addApi(createHttpApi(environment, server.port))
            val (url, websocket) = websocket(server, httpApi)

            server.stop()

            val closeEvent = pullEvent("websocket_closed")
            assertThat(
                "Websocket was closed",
                closeEvent,
                array(equalTo("websocket_closed"), equalTo(url), equalTo("Connection closed"), equalTo(null)),
            )

            assertThrows<LuaException>("Throws an exception when sending") {
                websocket.send(Coerced(LuaValues.encode("hello")), Optional.of(false))
            }

            Reference.reachabilityFence(websocket)
        }
    }

    @Test
    fun `Closes websocket if the server doesn't respond`() {
        runWithServer(closeWebsocket = false) { server, _ ->
            val httpApi = addApi(createHttpApi(environment, server.port))
            val (url, websocket) = websocket(server, httpApi)

            websocket.close()

            val closeEvent = pullEvent()
            assertThat("Received a close event", closeEvent, array(equalTo("websocket_closed"), equalTo(url), nullValue(), equalTo(1006)))

            Reference.reachabilityFence(websocket)
        }
    }

    private data class DelayStats(val send: Duration, val receive: Duration)

    /**
     * Send a sequence of random messages on a websocket, and time how long each message takes to send and receive.
     *
     * As bandwidth limits are somewhat non-deterministic (dependent on IO and thread scheduling), this function sends
     * multiple messages (as many as it can for 2 seconds), and takes the average delay over all of them.
     */
    private fun measureWebsocketDelay(config: NettyHttp.Config): DelayStats {
        var count = 0
        var send = Duration.ZERO
        var receive = Duration.ZERO

        val random = RandomGeneratorFactory.of<RandomGenerator>("L32X64MixRandom").create(-8780085030276499495L)

        /** Create a random message. This creates a new message every time to ensure messages do not compress. */
        fun randomMessage(): ByteBuffer {
            val message = ByteArray(BANDWIDTH_MESSAGE_SIZE)
            random.nextBytes(message)
            return ByteBuffer.wrap(message).asReadOnlyBuffer()
        }

        runWithServer { server, _ ->
            val httpApi = addApi(createHttpApi(environment, server.port, config))
            val (_, websocket) = websocket(server, httpApi)

            // Send/receive one message before timing. Read bandwidth limits get applied retroactively (we disable
            // auto-read after hitting the limit), so we need to ensure that happens first.
            websocket.send(Coerced(randomMessage()), Optional.of(true))
            websocket.receive(Optional.empty()).await()

            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
            do {
                val sent = System.nanoTime()
                websocket.send(Coerced(randomMessage()), Optional.of(true))
                websocket.receive(Optional.empty()).await()
                val clientReceived = System.nanoTime()
                val serverReceived = server.lastRequest.lastMessageTime

                count += 1
                send += (serverReceived - sent).nanoseconds
                receive += (clientReceived - serverReceived).nanoseconds
            } while (sent < deadline)

            websocket.close()
        }

        assertThat("Ran multiple tests", count, greaterThan(1))

        return DelayStats(send = send / count, receive = receive / count)
    }

    @Test
    fun `Messages are sent and received with no delay`() {
        val delay = measureWebsocketDelay(createHttpConfig())
        // When sending a message which does not hit the bandwidth cap, then we shouldn't see any delay on the message.
        assertThat("Send delay", delay.send, lessThan(INSTANT_DURATION))
        assertThat("Receive delay", delay.receive, lessThan(INSTANT_DURATION))
    }

    @Test
    fun `Limits upload bandwidth`() {
        val delay =
            measureWebsocketDelay(createHttpConfig(uploadBandwidth = BANDWIDTH_MESSAGE_SIZE * BANDWIDTH_SCALE))
        // When sending a message which does hit the bandwidth cap, then we should expect a delay in sending the message,
        // but not in receiving the reply.
        assertThat("Send delay", delay.send, greaterThan(LIMITED_DURATION))
        assertThat("Receive delay", delay.receive, lessThan(INSTANT_DURATION))
    }

    @Test
    fun `Limits download bandwidth`() {
        val delay =
            measureWebsocketDelay(createHttpConfig(downloadBandwidth = BANDWIDTH_MESSAGE_SIZE * BANDWIDTH_SCALE))
        // When sending a message which does hit the bandwidth cap, then we should expect a delay in sending the message,
        // but not in receiving the reply.
        assertThat("Send delay", delay.send, lessThan(INSTANT_DURATION))
        assertThat("Receive delay", delay.receive, greaterThan(LIMITED_DURATION))
    }

    companion object {
        /**
         * The length of a random websocket message used by our bandwidth tests
         *
         * @see measureWebsocketDelay
         */
        private const val BANDWIDTH_MESSAGE_SIZE: Int = 1024

        /**
         * A scaler on our bandwidth. Increasing this allows us to send more messages within [measureWebsocketDelay],
         * increasing the number of samples, but reducing the granularity of [INSTANT_DURATION] and [LIMITED_DURATION].
         */
        private const val BANDWIDTH_SCALE: Int = 2

        /** The duration when we've not hit the bandwidth limit and the message is sent "instantly". */
        private val INSTANT_DURATION: Duration = 50.milliseconds

        /** The duration when we've hit the bandwidth limit. */
        private val LIMITED_DURATION: Duration = 800.milliseconds / BANDWIDTH_SCALE
    }
}
