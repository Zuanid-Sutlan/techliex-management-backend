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

            configureMonitoring()
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
                orderNumber = "ORD-1001",
                orderDate = "15/01/2025",
                productId = productId,
                address = "123 Main St",
                note = "Custom keycaps variation",
                quantity = 2,
                listingPrice = 45.0,
                paymentScreenshot = "data:image/jpeg;base64,iVBORw0KGgo..."
            ))
        }
        assertEquals(HttpStatusCode.Created, createOrderResponse.status)
        val orderData = createOrderResponse.body<ApiResponse<OrderDto>>().data
        assertNotNull(orderData)
        val orderId = orderData.id

        assertEquals("ORD-1001", orderData.orderNumber)
        assertEquals("Wireless Keyboard", orderData.productName)
        assertEquals("data:image/jpeg;base64,iVBORw0KGgo...", orderData.productImage)
        assertEquals(20.0, orderData.productSourcePrice)
        assertEquals("Custom keycaps variation", orderData.note)
        assertEquals("data:image/jpeg;base64,iVBORw0KGgo...", orderData.paymentScreenshot)

        // 13b. Create Order without hunted product (Custom product order)
        val createCustomOrderResponse = client.post("/api/v1/orders") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateOrderRequest(
                orderNumber = "ORD-CUSTOM-01",
                orderDate = "16/01/2025",
                productId = null,
                productName = "Custom Handcrafted Deskmat",
                productImage = "https://example.com/deskmat.png",
                productSourcePrice = 15.0,
                address = "456 Custom Way",
                note = "XXL size",
                quantity = 1,
                listingPrice = 30.0,
                paymentScreenshot = "data:image/png;base64,abc..."
            ))
        }
        assertEquals(HttpStatusCode.Created, createCustomOrderResponse.status)
        val customOrderData = createCustomOrderResponse.body<ApiResponse<OrderDto>>().data
        assertNotNull(customOrderData)
        assertNull(customOrderData.productId)
        assertEquals("ORD-CUSTOM-01", customOrderData.orderNumber)
        assertEquals("Custom Handcrafted Deskmat", customOrderData.productName)
        assertEquals("https://example.com/deskmat.png", customOrderData.productImage)
        assertEquals(15.0, customOrderData.productSourcePrice)
        assertEquals(30.0, customOrderData.listingPrice)

        // 13c. Test Multi-Item Order Placement
        val createMultiOrderResponse = client.post("/api/v1/orders") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateOrderRequest(
                orderNumber = "ORD-MULTI-01",
                orderDate = "17/01/2025",
                address = "789 Multi St",
                paymentScreenshot = "data:image/png;base64,multi...",
                items = listOf(
                    CreateOrderItemRequest(
                        productId = productId,
                        quantity = 2,
                        note = "Red Switch Variation",
                        listingPrice = 50.0
                    ),
                    CreateOrderItemRequest(
                        productName = "Custom Mousepad",
                        productImage = "https://example.com/pad.png",
                        productSourcePrice = 10.0,
                        quantity = 3,
                        note = "900x400mm Size",
                        listingPrice = 25.0
                    )
                )
            ))
        }
        assertEquals(HttpStatusCode.Created, createMultiOrderResponse.status)
        val multiOrdersData = createMultiOrderResponse.body<ApiResponse<List<OrderDto>>>().data
        assertNotNull(multiOrdersData)
        assertEquals(2, multiOrdersData.size)
        assertEquals("ORD-MULTI-01", multiOrdersData[0].orderNumber)
        assertEquals(2, multiOrdersData[0].quantity)
        assertEquals("Red Switch Variation", multiOrdersData[0].note)
        assertEquals("Custom Mousepad", multiOrdersData[1].productName)
        assertEquals(3, multiOrdersData[1].quantity)

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
        val shippedOrder = updateLogisticsResponse.body<ApiResponse<OrderDto>>().data
        assertNotNull(shippedOrder)
        assertTrue(shippedOrder.updatedAt.isNotBlank())

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

            configureMonitoring()
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

        // User 3 (shared member) updates source link & reference link
        val u3UpdateLinksRes = client.patch("/api/v1/products/$productId/links") {
            header(HttpHeaders.Authorization, "Bearer $user3Token")
            contentType(ContentType.Application.Json)
            setBody(UpdateProductLinksRequest(
                sourceLink = "https://supplier.com/updated-by-u3",
                sourcePrice = 32.0,
                referenceLink = "https://amazon.com/updated-by-u3",
                referencePrice = 85.0
            ))
        }
        assertEquals(HttpStatusCode.OK, u3UpdateLinksRes.status)
        val updatedLinksProd = u3UpdateLinksRes.body<ApiResponse<ProductDto>>().data!!
        assertEquals("https://supplier.com/updated-by-u3", updatedLinksProd.sourceLink)
        assertEquals(32.0, updatedLinksProd.sourcePrice)
        assertEquals("https://amazon.com/updated-by-u3", updatedLinksProd.referenceLink)
        assertEquals(85.0, updatedLinksProd.referencePrice)

        // Admin updates warehouse price, note & picture
        val adminWhRes = client.patch("/api/v1/products/$productId/warehouse") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody(UpdateWarehouseRequest(
                warehousePrice = 35.0,
                warehouseNote = "Warehouse Checked",
                warehouseImage = "data:image/jpeg;base64,wh_proof_img"
            ))
        }
        assertEquals(HttpStatusCode.OK, adminWhRes.status)
        val whProduct = adminWhRes.body<ApiResponse<ProductDto>>().data!!
        assertEquals(35.0, whProduct.warehousePrice)
        assertEquals("Warehouse Checked", whProduct.warehouseNote)
        assertEquals("data:image/jpeg;base64,wh_proof_img", whProduct.warehouseImage)

        // Create an order linked to this product to test deletion with existing order references
        val createOrderRes = client.post("/api/v1/orders") {
            header(HttpHeaders.Authorization, "Bearer $user1Token")
            contentType(ContentType.Application.Json)
            setBody(CreateOrderRequest(
                orderNumber = "ORD-TEST-SHARED",
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

    @Test
    fun `test order refund chat flow and status management`() = testApplication {
        application {
            DatabaseFactory.init(environment.config)
            JwtConfig.init(environment.config)

            configureMonitoring()
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

        // Login Admin
        val adminLogin = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "umair", password = "umair123"))
        }
        val adminToken = adminLogin.body<ApiResponse<LoginResponse>>().data!!.token

        val ts = System.currentTimeMillis()
        val buyerName = "buyer_$ts"
        val outsiderName = "outsider_$ts"

        // Create Buyer and Outsider user
        client.post("/api/v1/users") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody(CreateUserRequest(username = buyerName, password = "pass123", name = "Buyer User", role = "Member"))
        }

        client.post("/api/v1/users") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody(CreateUserRequest(username = outsiderName, password = "pass123", name = "Outsider User", role = "Member"))
        }

        // Login as Buyer & Outsider
        val buyerLogin = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = buyerName, password = "pass123"))
        }
        val buyerToken = buyerLogin.body<ApiResponse<LoginResponse>>().data!!.token

        val outsiderLogin = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = outsiderName, password = "pass123"))
        }
        val outsiderToken = outsiderLogin.body<ApiResponse<LoginResponse>>().data!!.token

        // Create Product
        val createProdRes = client.post("/api/v1/products") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody(CreateProductRequest(
                title = "Bluetooth Headphones",
                description = "Noise Cancelling",
                productImageUrl = "data:image/png;base64,prod_img_base64",
                sourceLink = "https://supplier.com/hp",
                sourcePrice = 50.0,
                referenceLink = "https://store.com/hp",
                referencePrice = 120.0
            ))
        }
        val prodId = createProdRes.body<ApiResponse<ProductDto>>().data!!.id

        // Buyer creates Order
        val createOrderRes = client.post("/api/v1/orders") {
            header(HttpHeaders.Authorization, "Bearer $buyerToken")
            contentType(ContentType.Application.Json)
            setBody(CreateOrderRequest(
                orderNumber = "ORD-BUYER-999",
                productId = prodId,
                address = "789 Park Ave",
                note = "Blue color variation requested",
                quantity = 1,
                paymentScreenshot = "data:image/png;base64,payment_proof_123"
            ))
        }
        assertEquals(HttpStatusCode.Created, createOrderRes.status)
        val order = createOrderRes.body<ApiResponse<OrderDto>>().data!!
        val orderId = order.id

        assertEquals("ORD-BUYER-999", order.orderNumber)
        assertEquals("Bluetooth Headphones", order.productName)
        assertEquals("data:image/png;base64,prod_img_base64", order.productImage)
        assertEquals(50.0, order.productSourcePrice)
        assertEquals("Blue color variation requested", order.note)
        assertEquals("data:image/png;base64,payment_proof_123", order.paymentScreenshot)
        assertEquals("Active", order.status)

        // Buyer opens refund
        val openRefundRes = client.post("/api/v1/orders/$orderId/refund") {
            header(HttpHeaders.Authorization, "Bearer $buyerToken")
            contentType(ContentType.Application.Json)
            setBody(OpenRefundRequest(
                message = "Earphone left side non-functional",
                screenshots = listOf("data:image/png;base64,damaged_pic_1")
            ))
        }
        assertEquals(HttpStatusCode.OK, openRefundRes.status)
        val refundedOrder = openRefundRes.body<ApiResponse<OrderDto>>().data!!
        assertEquals("Refund Requested", refundedOrder.status)

        // Admin sends reply in refund chat
        val adminMsgRes = client.post("/api/v1/orders/$orderId/refund/messages") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody(SendRefundMessageRequest(
                message = "Sorry for the issue. Please share product serial number screenshot.",
                screenshots = listOf("data:image/png;base64,return_instructions")
            ))
        }
        assertEquals(HttpStatusCode.Created, adminMsgRes.status)

        // Buyer replies with screenshot
        val buyerReplyRes = client.post("/api/v1/orders/$orderId/refund/messages") {
            header(HttpHeaders.Authorization, "Bearer $buyerToken")
            contentType(ContentType.Application.Json)
            setBody(SendRefundMessageRequest(
                message = "Attached is the serial number barcode photo.",
                screenshots = listOf("data:image/png;base64,serial_barcode")
            ))
        }
        assertEquals(HttpStatusCode.Created, buyerReplyRes.status)

        // Fetch refund chat messages
        val getMessagesRes = client.get("/api/v1/orders/$orderId/refund/messages") {
            header(HttpHeaders.Authorization, "Bearer $buyerToken")
        }
        assertEquals(HttpStatusCode.OK, getMessagesRes.status)
        val messages = getMessagesRes.body<ApiResponse<List<RefundMessageDto>>>().data!!
        assertEquals(3, messages.size)
        assertEquals("Earphone left side non-functional", messages[0].message)
        assertTrue(messages[0].screenshots.contains("data:image/png;base64,damaged_pic_1"))

        assertEquals("Sorry for the issue. Please share product serial number screenshot.", messages[1].message)
        assertEquals("umair", messages[1].senderName)

        assertEquals("Attached is the serial number barcode photo.", messages[2].message)

        // Admin updates status to "Refund Approved"
        val statusUpdateRes = client.patch("/api/v1/orders/$orderId/status") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody(UpdateOrderStatusRequest(status = "Refund Approved"))
        }
        assertEquals(HttpStatusCode.OK, statusUpdateRes.status)
        assertEquals("Refund Approved", statusUpdateRes.body<ApiResponse<OrderDto>>().data!!.status)

        // Outsider attempts to view refund messages -> 403 Forbidden
        val outsiderAccessRes = client.get("/api/v1/orders/$orderId/refund/messages") {
            header(HttpHeaders.Authorization, "Bearer $outsiderToken")
        }
        assertEquals(HttpStatusCode.Forbidden, outsiderAccessRes.status)
    }

    @Test
    fun `test permanent deletion of users orders and chats`() = testApplication {
        application {
            DatabaseFactory.init(environment.config)
            JwtConfig.init(environment.config)

            configureMonitoring()
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

        // Login Admin
        val adminLogin = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "umair", password = "umair123"))
        }
        val adminToken = adminLogin.body<ApiResponse<LoginResponse>>().data!!.token

        val ts = System.currentTimeMillis()
        val tempUser = "del_user_$ts"

        // Create Temp User
        client.post("/api/v1/users") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody(CreateUserRequest(username = tempUser, password = "pass123", name = "Temp User", role = "Member"))
        }

        val tempLogin = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = tempUser, password = "pass123"))
        }
        val tempToken = tempLogin.body<ApiResponse<LoginResponse>>().data!!.token

        // Create Product for temp user
        val prodRes = client.post("/api/v1/products") {
            header(HttpHeaders.Authorization, "Bearer $tempToken")
            contentType(ContentType.Application.Json)
            setBody(CreateProductRequest(
                title = "Temp Speaker",
                description = "Portable",
                productImageUrl = "https://example.com/speaker.jpg",
                sourceLink = "https://supplier.com/sp",
                sourcePrice = 15.0,
                referenceLink = "https://store.com/sp",
                referencePrice = 40.0
            ))
        }
        val prodId = prodRes.body<ApiResponse<ProductDto>>().data!!.id

        // Temp User creates Order 1 & Order 2
        val o1Res = client.post("/api/v1/orders") {
            header(HttpHeaders.Authorization, "Bearer $tempToken")
            contentType(ContentType.Application.Json)
            setBody(CreateOrderRequest(
                orderNumber = "ORD-DEL-1",
                productId = prodId,
                address = "123 Del St",
                quantity = 1,
                paymentScreenshot = "data:image/png;base64,proof1"
            ))
        }
        val order1Id = o1Res.body<ApiResponse<OrderDto>>().data!!.id

        val o2Res = client.post("/api/v1/orders") {
            header(HttpHeaders.Authorization, "Bearer $tempToken")
            contentType(ContentType.Application.Json)
            setBody(CreateOrderRequest(
                orderNumber = "ORD-DEL-2",
                productId = prodId,
                address = "123 Del St",
                quantity = 2,
                paymentScreenshot = "data:image/png;base64,proof2"
            ))
        }
        val order2Id = o2Res.body<ApiResponse<OrderDto>>().data!!.id

        // Send chats on Order 1
        client.post("/api/v1/orders/$order1Id/refund") {
            header(HttpHeaders.Authorization, "Bearer $tempToken")
            contentType(ContentType.Application.Json)
            setBody(OpenRefundRequest(message = "Defective speaker", screenshots = listOf("data:image/png;base64,defect")))
        }

        client.post("/api/v1/orders/$order1Id/refund/messages") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody(SendRefundMessageRequest(message = "Please send photo", screenshots = emptyList()))
        }

        // Verify chats exist on Order 1
        val msgsBefore = client.get("/api/v1/orders/$order1Id/refund/messages") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(2, msgsBefore.body<ApiResponse<List<RefundMessageDto>>>().data!!.size)

        // 1. Test deleting only the chats against Order 1
        val delChatsRes = client.delete("/api/v1/orders/$order1Id/refund/messages") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, delChatsRes.status)

        val msgsAfterDelChats = client.get("/api/v1/orders/$order1Id/refund/messages") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(0, msgsAfterDelChats.body<ApiResponse<List<RefundMessageDto>>>().data!!.size)

        // 2. Test deleting Order 2 permanently
        val delOrder2Res = client.delete("/api/v1/orders/$order2Id") {
            header(HttpHeaders.Authorization, "Bearer $tempToken")
        }
        assertEquals(HttpStatusCode.OK, delOrder2Res.status)

        val checkOrder2 = client.get("/api/v1/orders/$order2Id") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.NotFound, checkOrder2.status)

        // 3. Test deleting Temp User permanently from database
        val delUserPermRes = client.delete("/api/v1/users/$tempUser/permanent") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, delUserPermRes.status)

        // Verify Temp User is gone from database
        val checkUserGone = client.get("/api/v1/users/$tempUser") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.NotFound, checkUserGone.status)
    }
}
