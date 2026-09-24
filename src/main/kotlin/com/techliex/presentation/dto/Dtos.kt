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
    sharedUsernames = sharedUsernames,
    createdAt = createdAt
)

@Serializable
data class CreateOrderRequest(
    val orderDate: String,
    val productId: Long,
    val address: String,
    val variationNote: String? = null,
    val quantity: Int,
    val listingPrice: Double,
    val paymentImageUrl: String
)

@Serializable
data class UpdateLogisticsRequest(
    val trackId: String,
    val company: String
)

@Serializable
data class OrderDto(
    val id: Long,
    val orderDate: String,
    val userId: Long,
    val userName: String? = null,
    val productId: Long,
    val productTitle: String? = null,
    val address: String,
    val variationNote: String?,
    val quantity: Int,
    val listingPrice: Double,
    val paymentImageUrl: String,
    val trackId: String,
    val company: String,
    val status: String,
    val createdAt: String
)

fun Order.toDto(): OrderDto = OrderDto(
    id = id,
    orderDate = orderDate,
    userId = userId,
    userName = userName,
    productId = productId,
    productTitle = productTitle,
    address = address,
    variationNote = variationNote,
    quantity = quantity,
    listingPrice = listingPrice,
    paymentImageUrl = paymentImageUrl,
    trackId = trackId,
    company = company,
    status = status,
    createdAt = createdAt
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
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

@Serializable
data class UploadResponse(
    val imageUrl: String
)
