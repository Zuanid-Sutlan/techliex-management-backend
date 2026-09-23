package com.techliex.domain.repository

import com.techliex.domain.model.DashboardStats

interface DashboardRepository {
    suspend fun getStatsForUser(userId: Long, userRole: String): DashboardStats
}
