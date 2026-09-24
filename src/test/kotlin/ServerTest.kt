package com.techliex

import com.techliex.data.db.DatabaseFactory
import com.techliex.plugins.*
import com.techliex.presentation.dto.*
import com.techliex.security.JwtConfig
import com.techliex.security.configureSecurity
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.*

class ServerTest {

    @Test
    fun `test complete API flow`() = testApplication {
        application {
            DatabaseFactory.init(environment.config)
            JwtConfig.init(environment.config)

            configureSerialization()
            configureSecurity()
            configureStatusPages()
            configureCORS()
            configureRouting()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }

        // 1. Root Endpoint
        val rootResponse = client.get("/")
        assertEquals(HttpStatusCode.OK, rootResponse.status)
        assertTrue(rootResponse.bodyAsText().contains("Techliex Management API Service"))

        // 2. Login as Admin (Umair)
        val loginResponse = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "umair", password = "umair123"))
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)
        val loginData = loginResponse.body<ApiResponse<LoginResponse>>().data
        assertNotNull(loginData)
        val token = loginData.token
        assertEquals("umair", loginData.user.username)
        assertEquals("Admin", loginData.user.role)

        // 3. Get Me Profile
        val meResponse = client.get("/api/v1/auth/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, meResponse.status)
        val meData = meResponse.body<ApiResponse<UserDto>>().data
        assertNotNull(meData)
        assertEquals("Umair", meData.name)

        // 4. Create New Member User
        val createUserResponse = client.post("/api/v1/users") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateUserRequest(username = "john_doe", password = "password123", name = "John Doe", role = "Member"))
        }
        assertEquals(HttpStatusCode.Created, createUserResponse.status)

        // 5. Get Users List
        val getUsersResponse = client.get("/api/v1/users") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, getUsersResponse.status)

        // 6. Create Product Hunt
        val createProductResponse = client.post("/api/v1/products") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateProductRequest(
                title = "Wireless Keyboard",
                description = "RGB Mechanical",
                productImageUrl = "/uploads/keyboard.jpg",
                sourceLink = "https://supplier.com/item/1",
                sourcePrice = 20.0,
                referenceLink = "https://amazon.com/item/1",
                referencePrice = 50.0,
                shareWithUsernames = listOf("john_doe")
            ))
        }
        assertEquals(HttpStatusCode.Created, createProductResponse.status)
        val productData = createProductResponse.body<ApiResponse<ProductDto>>().data
        assertNotNull(productData)
        val productId = productData.id

        // 7. Get Products List
        val getProductsResponse = client.get("/api/v1/products") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, getProductsResponse.status)

        // 8. Update Warehouse Info
        val updateWarehouseResponse = client.patch("/api/v1/products/$productId/warehouse") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(UpdateWarehouseRequest(warehousePrice = 25.0, warehouseNote = "In Stock"))
        }
        assertEquals(HttpStatusCode.OK, updateWarehouseResponse.status)

        // 9. Create Order
        val createOrderResponse = client.post("/api/v1/orders") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateOrderRequest(
                orderDate = "15/01/2025",
                productId = productId,
                address = "123 Main St",
                variationNote = "Color Black",
                quantity = 2,
                listingPrice = 45.0,
                paymentImageUrl = "/uploads/payment.jpg"
            ))
        }
        assertEquals(HttpStatusCode.Created, createOrderResponse.status)
        val orderData = createOrderResponse.body<ApiResponse<OrderDto>>().data
        assertNotNull(orderData)
        val orderId = orderData.id

        // 10. Get Orders List
        val getOrdersResponse = client.get("/api/v1/orders") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, getOrdersResponse.status)

        // 11. Update Logistics
        val updateLogisticsResponse = client.patch("/api/v1/orders/$orderId/logistics") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(UpdateLogisticsRequest(trackId = "TRK123456", company = "FedEx"))
        }
        assertEquals(HttpStatusCode.OK, updateLogisticsResponse.status)

        // 12. Complete Order
        val completeOrderResponse = client.patch("/api/v1/orders/$orderId/complete") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, completeOrderResponse.status)

        // 13. Get Dashboard Stats
        val statsResponse = client.get("/api/v1/dashboard/stats") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, statsResponse.status)
        val statsData = statsResponse.body<ApiResponse<DashboardStatsDto>>().data
        assertNotNull(statsData)
        assertTrue(statsData.productCount >= 1)
    }
}
