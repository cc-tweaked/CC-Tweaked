package dan200.computercraft.core.apis.http.options

import dan200.computercraft.ComputerCraft
import dan200.computercraft.core.apis.AsyncRunner
import dan200.computercraft.core.apis.HTTPAPI
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*

@Disabled("Requires some setup locally.")
class TestHttpApi {
    companion object {
        private const val WS_ADDRESS = "ws://127.0.0.1:8080"

        @JvmStatic
        @BeforeAll
        fun before() {
            ComputerCraft.httpRules = listOf(AddressRule.parse("*", null, Action.ALLOW.toPartial()))
        }

        @JvmStatic
        @AfterAll
        fun after() {
            ComputerCraft.httpRules = Collections.unmodifiableList(
                listOf(
                    AddressRule.parse("\$private", null, Action.DENY.toPartial()),
                    AddressRule.parse("*", null, Action.ALLOW.toPartial())
                )
            )
        }
    }

    @Test
    fun `Connects to websocket`() {
        AsyncRunner.runTest {
            val httpApi = addApi(HTTPAPI(this))

            val result = httpApi.websocket(WS_ADDRESS, Optional.empty())
            assertArrayEquals(arrayOf(true), result, "Should have created websocket")

            val event = pullEvent()
            assertEquals("websocket_success", event.name) {
                "Websocket failed to connect: ${event.args.contentToString()}"
            }
        }
    }
}
