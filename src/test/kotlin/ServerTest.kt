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
    fun `test database connection URL parsing with @ in password`() = testApplication {
        application {
            val (url, user, password) = DatabaseFactory.parseConnectionDetails(environment.config)
            assertTrue(url.startsWith("jdbc:postgresql://"))
            assertEquals("techliex", user)
            assertEquals("techliex@123", password)
        }
    }

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

        // 2. Health Check API Test (/health and /api/v1/health)
        val healthResponse = client.get("/health")
        assertEquals(HttpStatusCode.OK, healthResponse.status)
        val healthData = healthResponse.body<ApiResponse<HealthCheckDto>>().data
        assertNotNull(healthData)
        assertEquals("UP", healthData.serverStatus)
        assertEquals("CONNECTED", healthData.dbStatus)

        val apiHealthResponse = client.get("/api/v1/health")
        assertEquals(HttpStatusCode.OK, apiHealthResponse.status)
        val apiHealthData = apiHealthResponse.body<ApiResponse<HealthCheckDto>>().data
        assertNotNull(apiHealthData)
        assertEquals("UP", apiHealthData.serverStatus)
        assertEquals("CONNECTED", apiHealthData.dbStatus)

        // 3. App Version API Check
        val versionResponse = client.get("/api/v1/version")
        assertEquals(HttpStatusCode.OK, versionResponse.status)
        val versionData = versionResponse.body<ApiResponse<VersionDto>>().data
        assertNotNull(versionData)
        assertNotNull(versionData.versionName)
        assertTrue(versionData.versionCode >= 1)
        assertEquals("Techliex Management", versionData.name)

        // 4. Login as Admin (Umair)
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

        // 5. Get Me Profile
        val meResponse = client.get("/api/v1/auth/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, meResponse.status)
        val meData = meResponse.body<ApiResponse<UserDto>>().data
        assertNotNull(meData)
        assertEquals("Umair", meData.name)

        val testUser = "john_doe_${System.currentTimeMillis()}"

        // 6. Create New Member User
        val createUserResponse = client.post("/api/v1/users") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateUserRequest(username = testUser, password = "password123", name = "John Doe", role = "Member"))
        }
        assertEquals(HttpStatusCode.Created, createUserResponse.status)

        // 7. Get Users List
        val getUsersResponse = client.get("/api/v1/users") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, getUsersResponse.status)

        // 8. Change Password for current user
        val changePassResponse = client.patch("/api/v1/auth/change-password") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest(oldPassword = "umair123", newPassword = "umair123new"))
        }
        assertEquals(HttpStatusCode.OK, changePassResponse.status)

        // Revert password back for future tests
        client.patch("/api/v1/auth/change-password") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest(oldPassword = "umair123new", newPassword = "umair123"))
        }

        // 9. Admin Reset Password for target user
        val resetPassResponse = client.patch("/api/v1/users/$testUser/reset-password") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(ResetPasswordRequest(newPassword = "newpassword123"))
        }
        assertEquals(HttpStatusCode.OK, resetPassResponse.status)

        // 10. Admin Deactivate & Re-activate User
        val deactivateResponse = client.delete("/api/v1/users/$testUser") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, deactivateResponse.status)

        val activateResponse = client.patch("/api/v1/users/$testUser/activate") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, activateResponse.status)

        // 11. Create Product Hunt
        val createProductResponse = client.post("/api/v1/products") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateProductRequest(
                title = "Wireless Keyboard",
                description = "RGB Mechanical",
                productImageUrl = "data:image/jpeg;base64,iVBORw0KGgo...",
                sourceLink = "https://supplier.com/item/1",
                sourcePrice = 20.0,
                referenceLink = "https://amazon.com/item/1",
                referencePrice = 50.0,
                shareWithUsernames = listOf(testUser)
            ))
        }
        assertEquals(HttpStatusCode.Created, createProductResponse.status)
        val productData = createProductResponse.body<ApiResponse<ProductDto>>().data
        assertNotNull(productData)
        val productId = productData.id

        // 11. Get Products List
        val getProductsResponse = client.get("/api/v1/products") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, getProductsResponse.status)

        // 12. Update Warehouse Info
        val updateWarehouseResponse = client.patch("/api/v1/products/$productId/warehouse") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(UpdateWarehouseRequest(warehousePrice = 25.0, warehouseNote = "In Stock"))
        }
        assertEquals(HttpStatusCode.OK, updateWarehouseResponse.status)

        // 13. Create Order
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
                paymentImageUrl = "data:image/jpeg;base64,iVBORw0KGgo..."
            ))
        }
        assertEquals(HttpStatusCode.Created, createOrderResponse.status)
        val orderData = createOrderResponse.body<ApiResponse<OrderDto>>().data
        assertNotNull(orderData)
        val orderId = orderData.id

        // 14. Get Orders List
        val getOrdersResponse = client.get("/api/v1/orders") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, getOrdersResponse.status)

        // 15. Update Logistics
        val updateLogisticsResponse = client.patch("/api/v1/orders/$orderId/logistics") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(UpdateLogisticsRequest(trackId = "TRK123456", company = "FedEx"))
        }
        assertEquals(HttpStatusCode.OK, updateLogisticsResponse.status)

        // 16. Complete Order
        val completeOrderResponse = client.patch("/api/v1/orders/$orderId/complete") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, completeOrderResponse.status)

        // 17. Get Dashboard Stats
        val statsResponse = client.get("/api/v1/dashboard/stats") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, statsResponse.status)
        val statsData = statsResponse.body<ApiResponse<DashboardStatsDto>>().data
        assertNotNull(statsData)
        assertTrue(statsData.productCount >= 1)
    }

    @Test
    fun `test product permissions and visibility for admin and members`() = testApplication {
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

        // Login as Admin
        val adminLoginResponse = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "umair", password = "umair123"))
        }
        val adminToken = adminLoginResponse.body<ApiResponse<LoginResponse>>().data!!.token

        val ts = System.currentTimeMillis()
        val user1Name = "prod_user1_$ts"
        val user2Name = "prod_user2_$ts"
        val user3Name = "prod_user3_$ts"

        // Create Member 1, Member 2, Member 3
        val u1Res = client.post("/api/v1/users") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody(CreateUserRequest(username = user1Name, password = "pass123", name = "User 1", role = "Member"))
        }
        val u1Id = u1Res.body<ApiResponse<UserDto>>().data!!.id
        assertTrue(u1Id > 0)

        val u2Res = client.post("/api/v1/users") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody(CreateUserRequest(username = user2Name, password = "pass123", name = "User 2", role = "Member"))
        }
        val u2Id = u2Res.body<ApiResponse<UserDto>>().data!!.id

        val u3Res = client.post("/api/v1/users") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody(CreateUserRequest(username = user3Name, password = "pass123", name = "User 3", role = "Member"))
        }
        val u3Id = u3Res.body<ApiResponse<UserDto>>().data!!.id

        // Login as User 1 and User 3
        val u1Login = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = user1Name, password = "pass123"))
        }
        val user1Token = u1Login.body<ApiResponse<LoginResponse>>().data!!.token

        val u3Login = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = user3Name, password = "pass123"))
        }
        val user3Token = u3Login.body<ApiResponse<LoginResponse>>().data!!.token

        // User 1 creates product shared with user 2 using sharedUserIds
        val createProductRes = client.post("/api/v1/products") {
            header(HttpHeaders.Authorization, "Bearer $user1Token")
            contentType(ContentType.Application.Json)
            setBody(CreateProductRequest(
                title = "Smart Watch",
                description = "Fitness Tracker",
                productImageUrl = "https://example.com/watch.jpg",
                sourceLink = "https://supplier.com/watch",
                sourcePrice = 30.0,
                referenceLink = "https://amazon.com/watch",
                referencePrice = 80.0,
                sharedUserIds = listOf(u2Id)
            ))
        }
        assertEquals(HttpStatusCode.Created, createProductRes.status)
        val product = createProductRes.body<ApiResponse<ProductDto>>().data!!
        val productId = product.id

        assertTrue(u2Id in product.sharedUserIds)

        // User 3 attempts to view product -> should be 403 Forbidden
        val u3ViewRes = client.get("/api/v1/products/$productId") {
            header(HttpHeaders.Authorization, "Bearer $user3Token")
        }
        assertEquals(HttpStatusCode.Forbidden, u3ViewRes.status)

        // User 3 attempts to update warehouse info -> should be 403 Forbidden
        val u3WhRes = client.patch("/api/v1/products/$productId/warehouse") {
            header(HttpHeaders.Authorization, "Bearer $user3Token")
            contentType(ContentType.Application.Json)
            setBody(UpdateWarehouseRequest(warehousePrice = 40.0, warehouseNote = "Unpermitted"))
        }
        assertEquals(HttpStatusCode.Forbidden, u3WhRes.status)

        // User 3 attempts to delete product -> should be 403 Forbidden
        val u3DelRes = client.delete("/api/v1/products/$productId") {
            header(HttpHeaders.Authorization, "Bearer $user3Token")
        }
        assertEquals(HttpStatusCode.Forbidden, u3DelRes.status)

        // Admin updates product shares so User 3 is included in sharedUserIds
        val adminUpdateSharesRes = client.patch("/api/v1/products/$productId/shares") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody(UpdateProductSharesRequest(sharedUserIds = listOf(u2Id, u3Id)))
        }
        assertEquals(HttpStatusCode.OK, adminUpdateSharesRes.status)
        val updatedProduct = adminUpdateSharesRes.body<ApiResponse<ProductDto>>().data!!
        assertTrue(u3Id in updatedProduct.sharedUserIds)

        // Now User 3 attempts to view product -> should be 200 OK
        val u3ViewSuccessRes = client.get("/api/v1/products/$productId") {
            header(HttpHeaders.Authorization, "Bearer $user3Token")
        }
        assertEquals(HttpStatusCode.OK, u3ViewSuccessRes.status)

        // Admin updates warehouse price & note
        val adminWhRes = client.patch("/api/v1/products/$productId/warehouse") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody(UpdateWarehouseRequest(warehousePrice = 35.0, warehouseNote = "Warehouse Checked"))
        }
        assertEquals(HttpStatusCode.OK, adminWhRes.status)
        val whProduct = adminWhRes.body<ApiResponse<ProductDto>>().data!!
        assertEquals(35.0, whProduct.warehousePrice)
        assertEquals("Warehouse Checked", whProduct.warehouseNote)

        // Create an order linked to this product to test deletion with existing order references
        val createOrderRes = client.post("/api/v1/orders") {
            header(HttpHeaders.Authorization, "Bearer $user1Token")
            contentType(ContentType.Application.Json)
            setBody(CreateOrderRequest(
                orderDate = "2025-01-01",
                productId = productId,
                address = "456 Test Street",
                quantity = 1,
                listingPrice = 80.0,
                paymentImageUrl = "data:image/png;base64,iVBOR..."
            ))
        }
        assertEquals(HttpStatusCode.Created, createOrderRes.status)

        // Admin deletes product
        val adminDelRes = client.delete("/api/v1/products/$productId") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, adminDelRes.status)

        // Check product is gone
        val checkGoneRes = client.get("/api/v1/products/$productId") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.NotFound, checkGoneRes.status)
    }
}
