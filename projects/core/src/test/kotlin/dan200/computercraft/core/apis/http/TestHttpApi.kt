// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http

import dan200.computercraft.api.lua.Coerced
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaValues
import dan200.computercraft.api.lua.ObjectArguments
import dan200.computercraft.core.CoreConfig
import dan200.computercraft.core.apis.HTTPAPI
import dan200.computercraft.core.apis.handles.ReadHandle
import dan200.computercraft.core.apis.http.HttpServer.Companion.runServer
import dan200.computercraft.core.apis.http.options.Action
import dan200.computercraft.core.apis.http.options.AddressRule
import dan200.computercraft.core.apis.http.request.HttpResponseHandle
import dan200.computercraft.core.apis.http.websocket.WebsocketHandle
import dan200.computercraft.test.core.computer.LuaTaskRunner
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

class TestHttpApi {
    companion object {
        @JvmStatic
        @BeforeAll
        fun before() {
            CoreConfig.httpRules = listOf(AddressRule.parse("*", OptionalInt.empty(), Action.ALLOW.toPartial()))
        }

        @JvmStatic
        @AfterAll
        fun after() {
            CoreConfig.httpRules = Collections.unmodifiableList(
                listOf(
                    AddressRule.parse("\$private", OptionalInt.empty(), Action.DENY.toPartial()),
                    AddressRule.parse("*", OptionalInt.empty(), Action.ALLOW.toPartial()),
                ),
            )
        }
    }

    @Test
    fun `Connects to a HTTP server`() {
        runServer { server ->
            LuaTaskRunner.runTest {
                val url = "http://127.0.0.1:${server.port}"
                val httpApi = addApi(HTTPAPI(environment))
                assertThat("http.request succeeded", httpApi.request(ObjectArguments(url)), array(equalTo(true)))

                val result = pullEvent("http_success")
                assertThat(result, array(equalTo("http_success"), equalTo(url), isA(HttpResponseHandle::class.java)))

                val handle = result[2] as HttpResponseHandle
                val reader = handle.extra.iterator().next() as ReadHandle
                assertThat(reader.readAll(), array(equalTo("Hello, world!".toByteArray())))
            }
        }
    }

    @Test
    fun `Connects to websocket`() {
        runServer { server ->
            LuaTaskRunner.runTest {
                val url = "ws://127.0.0.1:${server.port}/ws"
                val httpApi = addApi(HTTPAPI(environment))
                assertThat("http.websocket succeeded", httpApi.websocket(ObjectArguments(url)), array(equalTo(true)))

                val connectEvent = pullEvent()
                assertThat(connectEvent, array(equalTo("websocket_success"), equalTo(url), isA(WebsocketHandle::class.java)))

                val websocket = connectEvent[2] as WebsocketHandle
                websocket.send(Coerced(LuaValues.encode("Hello")), Optional.of(false))

                val message = websocket.receive(Optional.empty()).await()
                assertThat("Received a return message", message, array(equalTo("HELLO".toByteArray()), equalTo(false)))

                websocket.close()

                val closeEvent = pullEventOrTimeout(500.milliseconds, "websocket_closed")
                assertThat("No event was queued", closeEvent, equalTo(null))
            }
        }
    }

    @Test
    fun `Errors if too many websocket messages are sent`() {
        runServer { server ->
            LuaTaskRunner.runTest {
                val url = "ws://127.0.0.1:${server.port}/ws"
                val httpApi = addApi(HTTPAPI(environment))
                assertThat("http.websocket succeeded", httpApi.websocket(ObjectArguments(url)), array(equalTo(true)))

                val connectEvent = pullEvent()
                assertThat(connectEvent, array(equalTo("websocket_success"), equalTo(url), isA(WebsocketHandle::class.java)))

                val websocket = connectEvent[2] as WebsocketHandle
                val error = assertThrows<LuaException> {
                    for (i in 0 until 10_000) {
                        websocket.send(Coerced(LuaValues.encode("Hello")), Optional.of(false))
                    }
                }

                websocket.close()

                assertThat(error.message, equalTo("Too many ongoing websocket messages"))
            }
        }
    }

    @Test
    fun `Closes if a websocket message is too large`() {
        runServer { server ->
            LuaTaskRunner.runTest {
                val url = "ws://127.0.0.1:${server.port}/ws"
                val httpApi = addApi(HTTPAPI(environment))
                assertThat("http.websocket succeeded", httpApi.websocket(ObjectArguments(url)), array(equalTo(true)))

                val connectEvent = pullEvent()
                assertThat(connectEvent, array(equalTo("websocket_success"), equalTo(url), isA(WebsocketHandle::class.java)))

                val out = ByteArray(AddressRule.WEBSOCKET_MESSAGE + 1)
                Random(0xDEADBEEF).nextBytes(out)
                server.broadcast(BinaryWebSocketFrame(Unpooled.wrappedBuffer(out)))

                val closeEvent = pullEvent()
                assertThat(closeEvent, array(equalTo("websocket_closed"), equalTo(url), equalTo("Received a too-large message"), nullValue()))
            }
        }
    }

    @Test
    fun `Queues an event when the socket is externally closed`() {
        runServer { server ->
            LuaTaskRunner.runTest {
                val url = "ws://127.0.0.1:${server.port}/ws"
                val httpApi = addApi(HTTPAPI(environment))
                assertThat("http.websocket succeeded", httpApi.websocket(ObjectArguments(url)), array(equalTo(true)))

                val connectEvent = pullEvent()
                assertThat(connectEvent, array(equalTo("websocket_success"), equalTo(url), isA(WebsocketHandle::class.java)))

                val websocket = connectEvent[2] as WebsocketHandle

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
            }
        }
    }
}
