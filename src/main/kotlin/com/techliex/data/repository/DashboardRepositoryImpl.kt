package com.techliex.data.repository

import com.techliex.data.db.*
import com.techliex.domain.model.DashboardStats
import com.techliex.domain.repository.DashboardRepository
import org.jetbrains.exposed.sql.*

class DashboardRepositoryImpl : DashboardRepository {

    override suspend fun getStatsForUser(userId: Long, userRole: String): DashboardStats = DatabaseFactory.dbQuery {
        if (userRole == "Admin" || userRole == "Warehouse") {
            val totalProducts = ProductsTable.selectAll().count()
            val activeOrders = OrdersTable.selectAll().where { OrdersTable.status neq "Completed" }.count()
            val totalEarnings = OrdersTable.selectAll()
                .where { OrdersTable.status eq "Completed" }
                .sumOf { row ->
                    val price = row[OrdersTable.listingPrice].toDouble()
                    val qty = row[OrdersTable.quantity]
                    price * qty
                }

            DashboardStats(
                productCount = totalProducts,
                activeOrdersCount = activeOrders,
                totalEarnings = totalEarnings
            )
        } else {
            val ownProductCount = ProductsTable.selectAll()
                .where { ProductsTable.userId eq userId }
                .count()

            val sharedProductCount = ProductSharesTable.selectAll()
                .where { ProductSharesTable.sharedUserId eq userId }
                .count()

            val totalProducts = ownProductCount + sharedProductCount

            val activeOrders = OrdersTable.selectAll()
                .where { (OrdersTable.userId eq userId) and (OrdersTable.status neq "Completed") }
                .count()

            val totalEarnings = OrdersTable.selectAll()
                .where { (OrdersTable.userId eq userId) and (OrdersTable.status eq "Completed") }
                .sumOf { row ->
                    val price = row[OrdersTable.listingPrice].toDouble()
                    val qty = row[OrdersTable.quantity]
                    price * qty
                }

            DashboardStats(
                productCount = totalProducts,
                activeOrdersCount = activeOrders,
                totalEarnings = totalEarnings
            )
        }
    }
}
