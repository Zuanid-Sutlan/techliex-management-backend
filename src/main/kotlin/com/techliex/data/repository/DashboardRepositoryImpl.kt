package com.techliex.data.repository

import com.techliex.data.db.*
import com.techliex.data.db.table.OrdersTable
import com.techliex.data.db.table.ProductsTable
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
            val totalProducts = ProductsTable.selectAll()
                .map { row ->
                    val creatorId = row[ProductsTable.userId]
                    val sharedUserIds = row[ProductsTable.sharedUserIds].split(",").mapNotNull { it.trim().toLongOrNull() }
                    creatorId == userId || userId in sharedUserIds
                }
                .count { it }
                .toLong()

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
