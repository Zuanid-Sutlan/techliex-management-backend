package com.techliex.domain.model

data class DashboardStats(
    val productCount: Long,
    val activeOrdersCount: Long,
    val totalEarnings: Double,
    val pendingWarehouseCount: Long = 0,
    val pendingTrackingCount: Long = 0,
    val unreadSupportCount: Long = 0
)
