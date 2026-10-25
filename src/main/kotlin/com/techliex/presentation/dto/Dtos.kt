package com.techliex.presentation.dto

import com.techliex.domain.model.*
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserDto
)

@Serializable
data class CreateUserRequest(
    val username: String,
    val password: String,
    val name: String,
    val role: String = "Member"
)

@Serializable
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

@Serializable
data class ResetPasswordRequest(
    val newPassword: String
)

@Serializable
data class UserDto(
    val id: Long = -1,
    val username: String = "",
    val password: String = "",
    val name: String = "",
    val role: String = "",
    val isActive: Boolean = false,
    val createdAt: String = ""
)

fun User.toDto(): UserDto = UserDto(
    id = id,
    username = username,
    password = password,
    name = name,
    role = role,
    isActive = isActive,
    createdAt = createdAt
)

@Serializable
data class CreateProductRequest(
    val title: String,
    val description: String? = null,
    val productImageUrl: String,
    val sourceLink: String,
    val sourcePrice: Double,
    val referenceLink: String,
    val referencePrice: Double,
    val sharedUserIds: List<Long> = emptyList(),
    val shareWithUsernames: List<String> = emptyList()
)

@Serializable
data class UpdateProductRequest(
    val title: String? = null,
    val description: String? = null,
    val productImageUrl: String? = null,
    val sourceLink: String? = null,
    val sourcePrice: Double? = null,
    val referenceLink: String? = null,
    val referencePrice: Double? = null,
    val sharedUserIds: List<Long>? = null,
    val shareWithUsernames: List<String>? = null
)

@Serializable
data class UpdateProductSharesRequest(
    val sharedUserIds: List<Long> = emptyList(),
    val shareWithUsernames: List<String> = emptyList()
)

@Serializable
data class UpdateWarehouseRequest(
    val warehousePrice: Double,
    val warehouseNote: String? = null
)

@Serializable
data class ProductDto(
    val id: Long,
    val userId: Long,
    val creatorName: String? = null,
    val title: String,
    val description: String?,
    val productImageUrl: String,
    val sourceLink: String,
    val sourcePrice: Double,
    val referenceLink: String,
    val referencePrice: Double,
    val warehousePrice: Double,
    val warehouseNote: String?,
    val sharedUserIds: List<Long> = emptyList(),
    val sharedUsernames: List<String> = emptyList(),
    val createdAt: String
)

fun Product.toDto(): ProductDto = ProductDto(
    id = id,
    userId = userId,
    creatorName = creatorName,
    title = title,
    description = description,
    productImageUrl = productImageUrl,
    sourceLink = sourceLink,
    sourcePrice = sourcePrice,
    referenceLink = referenceLink,
    referencePrice = referencePrice,
    warehousePrice = warehousePrice,
    warehouseNote = warehouseNote,
    sharedUserIds = sharedUserIds,
    sharedUsernames = sharedUsernames,
    createdAt = createdAt
)

@Serializable
data class CreateOrderRequest(
    val orderNumber: String = "",
    val orderDate: String? = null,
    val productId: Long,
    val address: String,
    val note: String? = null,
    val variationNote: String? = null,
    val quantity: Int = 1,
    val listingPrice: Double? = null,
    val paymentScreenshot: String? = null,
    val paymentImageUrl: String? = null
)

@Serializable
data class UpdateLogisticsRequest(
    val trackId: String,
    val company: String
)

@Serializable
data class UpdateOrderStatusRequest(
    val status: String
)

@Serializable
data class OpenRefundRequest(
    val message: String? = null,
    val screenshots: List<String> = emptyList()
)

@Serializable
data class SendRefundMessageRequest(
    val message: String,
    val screenshots: List<String> = emptyList()
)

@Serializable
data class RefundMessageDto(
    val id: Long,
    val orderId: Long,
    val senderId: Long,
    val senderName: String,
    val senderRole: String,
    val message: String,
    val screenshots: List<String> = emptyList(),
    val createdAt: String
)

fun RefundMessage.toDto(): RefundMessageDto = RefundMessageDto(
    id = id,
    orderId = orderId,
    senderId = senderId,
    senderName = senderName,
    senderRole = senderRole,
    message = message,
    screenshots = screenshots,
    createdAt = createdAt
)

@Serializable
data class OrderDto(
    val id: Long,
    val orderNumber: String = "",
    val orderDate: String = "",
    val userId: Long,
    val userName: String? = null,
    val productId: Long,
    val productName: String = "",
    val productImage: String = "",
    val productSourcePrice: Double = 0.0,
    val productTitle: String? = null,
    val address: String,
    val note: String? = null,
    val variationNote: String? = null,
    val quantity: Int,
    val listingPrice: Double,
    val paymentScreenshot: String = "",
    val paymentImageUrl: String? = null,
    val trackId: String = "",
    val company: String = "",
    val status: String = "Active",
    val createdAt: String = "",
    val updatedAt: String = ""
)

fun Order.toDto(): OrderDto = OrderDto(
    id = id,
    orderNumber = orderNumber,
    orderDate = orderDate,
    userId = userId,
    userName = userName,
    productId = productId,
    productName = productName,
    productImage = productImage,
    productSourcePrice = productSourcePrice,
    productTitle = productTitle ?: productName,
    address = address,
    note = note ?: variationNote,
    variationNote = variationNote ?: note,
    quantity = quantity,
    listingPrice = listingPrice,
    paymentScreenshot = paymentScreenshot.ifEmpty { paymentImageUrl ?: "" },
    paymentImageUrl = paymentImageUrl ?: paymentScreenshot,
    trackId = trackId,
    company = company,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt.ifEmpty { createdAt }
)

@Serializable
data class DashboardStatsDto(
    val productCount: Long,
    val activeOrdersCount: Long,
    val totalEarnings: Double
)

fun DashboardStats.toDto(): DashboardStatsDto = DashboardStatsDto(
    productCount = productCount,
    activeOrdersCount = activeOrdersCount,
    totalEarnings = totalEarnings
)

@Serializable
data class VersionDto(
    val versionName: String,
    val versionCode: Int,
    val name: String
)

fun AppVersion.toDto(): VersionDto = VersionDto(
    versionName = versionName,
    versionCode = versionCode,
    name = name
)

@Serializable
data class HealthCheckDto(
    val serverStatus: String,
    val dbStatus: String,
    val databaseType: String,
    val dbMessage: String,
    val timestamp: String
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

@Serializable
data class UploadResponse(
    val imageUrl: String
)
