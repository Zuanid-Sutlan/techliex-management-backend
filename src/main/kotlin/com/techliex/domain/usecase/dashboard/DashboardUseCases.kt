package com.techliex.domain.usecase.dashboard

import com.techliex.domain.model.DashboardStats
import com.techliex.domain.repository.DashboardRepository

class GetDashboardStatsUseCase(private val dashboardRepository: DashboardRepository) {
    suspend operator fun invoke(userId: Long, userRole: String): DashboardStats {
        return dashboardRepository.getStatsForUser(userId, userRole)
    }
}
