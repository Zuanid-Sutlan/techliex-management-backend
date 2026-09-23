package com.techliex.data.repository

import com.techliex.data.db.*
import com.techliex.domain.model.Order
import com.techliex.domain.repository.OrderRepository
import org.jetbrains.exposed.sql.*
import java.math.BigDecimal
import java.time.Instant

class OrderRepositoryImpl : OrderRepository {

    override suspend fun createOrder(
        userId: Long,
        orderDate: String,
        productId: Long,
        address: String,
        variationNote: String?,
        quantity: Int,
        listingPrice: Double,
        paymentImageUrl: String
    ): Order = DatabaseFactory.dbQuery {
        val now = Instant.now().toString()
        val generatedId = OrdersTable.insert {
            it[OrdersTable.orderDate] = orderDate
            it[OrdersTable.userId] = userId
            it[OrdersTable.productId] = productId
            it[OrdersTable.address] = address
            it[OrdersTable.variationNote] = variationNote
            it[OrdersTable.quantity] = quantity
            it[OrdersTable.listingPrice] = BigDecimal.valueOf(listingPrice)
            it[OrdersTable.paymentImageUrl] = paymentImageUrl
            it[trackId] = ""
            it[company] = ""
            it[status] = "Active"
            it[createdAt] = now
        } get OrdersTable.id

        getOrderByIdInternal(generatedId)!!
    }

    override suspend fun getOrdersForUser(userId: Long, userRole: String): List<Order> = DatabaseFactory.dbQuery {
        val query = if (userRole == "Admin" || userRole == "Warehouse") {
            OrdersTable.selectAll()
        } else {
            OrdersTable.selectAll().where { OrdersTable.userId eq userId }
        }

        val orders = query.map { rowToOrder(it) }

        orders.sortedWith(Comparator { o1, o2 ->
            val priority = mapOf("Active" to 1, "Shipped" to 2, "Completed" to 3)
            val p1 = priority[o1.status] ?: 4
            val p2 = priority[o2.status] ?: 4
            p1.compareTo(p2)
        })
    }

    override suspend fun getOrderById(orderId: Long): Order? = DatabaseFactory.dbQuery {
        getOrderByIdInternal(orderId)
    }

    override suspend fun updateLogistics(orderId: Long, trackId: String, company: String): Order? = DatabaseFactory.dbQuery {
        val updatedRows = OrdersTable.update({ OrdersTable.id eq orderId }) {
            it[OrdersTable.trackId] = trackId
            it[OrdersTable.company] = company
            it[status] = "Shipped"
        }

        if (updatedRows > 0) getOrderByIdInternal(orderId) else null
    }

    override suspend fun completeOrder(orderId: Long): Order? = DatabaseFactory.dbQuery {
        val updatedRows = OrdersTable.update({ OrdersTable.id eq orderId }) {
            it[status] = "Completed"
        }

        if (updatedRows > 0) getOrderByIdInternal(orderId) else null
    }

    private fun getOrderByIdInternal(orderId: Long): Order? {
        return OrdersTable.selectAll()
            .where { OrdersTable.id eq orderId }
            .map { rowToOrder(it) }
            .singleOrNull()
    }

    private fun rowToOrder(row: ResultRow): Order {
        val orderUserId = row[OrdersTable.userId]
        val orderProductId = row[OrdersTable.productId]

        val userName = UsersTable.selectAll()
            .where { UsersTable.id eq orderUserId }
            .map { it[UsersTable.name] }
            .singleOrNull()

        val productTitle = ProductsTable.selectAll()
            .where { ProductsTable.id eq orderProductId }
            .map { it[ProductsTable.title] }
            .singleOrNull()

        return Order(
            id = row[OrdersTable.id],
            orderDate = row[OrdersTable.orderDate],
            userId = orderUserId,
            userName = userName,
            productId = orderProductId,
            productTitle = productTitle,
            address = row[OrdersTable.address],
            variationNote = row[OrdersTable.variationNote],
            quantity = row[OrdersTable.quantity],
            listingPrice = row[OrdersTable.listingPrice].toDouble(),
            paymentImageUrl = row[OrdersTable.paymentImageUrl],
            trackId = row[OrdersTable.trackId],
            company = row[OrdersTable.company],
            status = row[OrdersTable.status],
            createdAt = row[OrdersTable.createdAt]
        )
    }
}
