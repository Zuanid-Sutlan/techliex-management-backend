package com.example

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.*

class ServerTest {

    @Test
    fun `test root endpoint`() = testApplication {
        application {
            configureRouting()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Techliex Management API Service"))
    }
}
