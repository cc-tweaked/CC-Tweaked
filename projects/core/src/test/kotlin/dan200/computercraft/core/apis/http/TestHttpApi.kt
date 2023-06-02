// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http

import dan200.computercraft.api.lua.Coerced
import dan200.computercraft.api.lua.ObjectArguments
import dan200.computercraft.core.CoreConfig
import dan200.computercraft.core.apis.HTTPAPI
import dan200.computercraft.core.apis.handles.EncodedReadableHandle
import dan200.computercraft.core.apis.http.HttpServer.URL
import dan200.computercraft.core.apis.http.HttpServer.WS_URL
import dan200.computercraft.core.apis.http.HttpServer.runServer
import dan200.computercraft.core.apis.http.options.Action
import dan200.computercraft.core.apis.http.options.AddressRule
import dan200.computercraft.core.apis.http.request.HttpResponseHandle
import dan200.computercraft.core.apis.http.websocket.WebsocketHandle
import dan200.computercraft.test.core.computer.LuaTaskRunner
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*

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
        runServer {
            LuaTaskRunner.runTest {
                val httpApi = addApi(HTTPAPI(environment))
                assertThat("http.request succeeded", httpApi.request(ObjectArguments(URL)), array(equalTo(true)))

                val result = pullEvent("http_success")
                assertThat(result, array(equalTo("http_success"), equalTo(URL), isA(HttpResponseHandle::class.java)))

                val handle = result[2] as HttpResponseHandle
                val reader = handle.extra.iterator().next() as EncodedReadableHandle
                assertThat(reader.readAll(), array(equalTo("Hello, world!")))
            }
        }
    }

    @Test
    fun `Connects to websocket`() {
        runServer {
            LuaTaskRunner.runTest {
                val httpApi = addApi(HTTPAPI(environment))
                assertThat("http.websocket succeeded", httpApi.websocket(ObjectArguments(WS_URL)), array(equalTo(true)))

                val connectEvent = pullEvent()
                assertThat(connectEvent, array(equalTo("websocket_success"), equalTo(WS_URL), isA(WebsocketHandle::class.java)))

                val websocket = connectEvent[2] as WebsocketHandle
                websocket.send(Coerced("Hello"), Optional.of(false))

                val message = websocket.receive(Optional.empty()).await()
                assertThat("Received a return message", message, array(equalTo("HELLO"), equalTo(false)))

                websocket.close()

                val closeEvent = pullEvent("websocket_closed")
                assertThat(
                    "Websocket was closed",
                    closeEvent,
                    array(equalTo("websocket_closed"), equalTo(WS_URL), equalTo("Connection closed"), equalTo(null)),
                )
            }
        }
    }
}
