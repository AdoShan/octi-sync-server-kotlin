package eu.darken.octi.kserver.status

import eu.darken.octi.TestRunner
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import org.junit.jupiter.api.Test

class StatusFlowTest : TestRunner() {

    @Test
    fun `get status`() = runTest2 {
        http.get("/v1/status") {

        }.apply {
            status shouldBe HttpStatusCode.OK
        }
    }
}