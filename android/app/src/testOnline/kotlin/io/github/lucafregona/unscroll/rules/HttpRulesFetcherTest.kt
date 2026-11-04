package io.github.lucafregona.unscroll.rules

import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class HttpRulesFetcherTest {
    private val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0).apply {
        createContext("/ok") { ex -> ex.respond(200, "hello rules") }
        createContext("/missing") { ex -> ex.respond(404, "nope") }
        createContext("/redirect") { ex ->
            ex.responseHeaders.add("Location", "/ok")
            ex.respond(302, "")
        }
        createContext("/agent") { ex -> ex.respond(200, ex.requestHeaders.getFirst("User-Agent") ?: "none") }
        createContext("/huge") { ex -> ex.respond(200, "x".repeat(MAX_RULES_BYTES + 1)) }
        createContext("/limit") { ex -> ex.respond(200, "x".repeat(MAX_RULES_BYTES)) }
        start()
    }
    private val fetcher = HttpRulesFetcher()

    @After fun stop() = server.stop(0)

    private fun com.sun.net.httpserver.HttpExchange.respond(status: Int, body: String) {
        val bytes = body.toByteArray()
        sendResponseHeaders(status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
        if (bytes.isNotEmpty()) responseBody.use { it.write(bytes) } else close()
    }

    private fun url(path: String) = "http://127.0.0.1:${server.address.port}$path"

    private fun assertFails(path: String) {
        try {
            fetcher.get(url(path))
            fail("expected an IOException for $path")
        } catch (expected: IOException) {
            assertNotNull(expected.message)
        }
    }

    @Test fun a200ReturnsTheBody() = assertEquals("hello rules", fetcher.get(url("/ok")))
    @Test fun itIdentifiesItselfAsUnscrollAndNothingMore() = assertEquals("unscroll", fetcher.get(url("/agent")))
    @Test fun a404Throws() = assertFails("/missing")
    @Test fun aRedirectIsNotFollowed() = assertFails("/redirect")
    @Test fun aBodyAboveTheLimitThrows() = assertFails("/huge")

    @Test
    fun aBodyAtTheLimitIsAccepted() = assertTrue(fetcher.get(url("/limit")).length == MAX_RULES_BYTES)
}
