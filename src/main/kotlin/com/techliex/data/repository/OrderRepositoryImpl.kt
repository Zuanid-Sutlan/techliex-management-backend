package com.techliex.data.repository

import com.techliex.data.db.*
import com.techliex.data.db.table.OrdersTable
import com.techliex.data.db.table.ProductsTable
import com.techliex.data.db.table.RefundMessagesTable
import com.techliex.data.db.table.UsersTable
import com.techliex.domain.model.Order
import com.techliex.domain.model.RefundMessage
import com.techliex.domain.repository.OrderRepository
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.Instant

class OrderRepositoryImpl : OrderRepository {

    override suspend fun createOrder(
        userId: Long,
        orderNumber: String?,
        orderDate: String?,
        productId: Long,
        address: String,
        note: String?,
        quantity: Int,
        listingPrice: Double?,
        paymentScreenshot: String?
    ): Order = DatabaseFactory.dbQuery {
        val productRow = ProductsTable.selectAll()
            .where { ProductsTable.id eq productId }
            .singleOrNull()
            ?: throw IllegalArgumentException("Product not found with ID: $productId")

        val pTitle = productRow[ProductsTable.title]
        val pImg = productRow[ProductsTable.productImageUrl]
        val pSourcePrice = productRow[ProductsTable.sourcePrice]
        val pRefPrice = productRow[ProductsTable.referencePrice]

        val effectiveListingPrice = listingPrice ?: pRefPrice.toDouble()
        val effectiveOrderDate = if (!orderDate.isNullOrBlank()) orderDate else Instant.now().toString().take(10)
        val effectiveScreenshot = paymentScreenshot ?: ""
        val effectiveOrderNumber = orderNumber ?: ""
        val now = Instant.now().toString()

        val generatedId = OrdersTable.insert {
            it[OrdersTable.orderNumber] = effectiveOrderNumber
            it[OrdersTable.orderDate] = effectiveOrderDate
            it[OrdersTable.userId] = userId
            it[OrdersTable.productId] = productId
            it[OrdersTable.productName] = pTitle
            it[OrdersTable.productImage] = pImg
            it[OrdersTable.productSourcePrice] = pSourcePrice
            it[OrdersTable.address] = address
            it[OrdersTable.note] = note
            it[OrdersTable.variationNote] = note
            it[OrdersTable.quantity] = quantity
            it[OrdersTable.listingPrice] = BigDecimal.valueOf(effectiveListingPrice)
            it[OrdersTable.paymentScreenshot] = effectiveScreenshot
            it[OrdersTable.paymentImageUrl] = effectiveScreenshot
            it[OrdersTable.trackId] = ""
            it[OrdersTable.company] = ""
            it[OrdersTable.status] = "Active"
            it[OrdersTable.createdAt] = now
            it[OrdersTable.updatedAt] = now
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
            val priority = mapOf("Active" to 1, "Shipped" to 2, "Completed" to 3, "Refund Requested" to 4, "Refund Pending" to 5)
            val p1 = priority[o1.status] ?: 6
            val p2 = priority[o2.status] ?: 6
            p1.compareTo(p2)
        })
    }

    override suspend fun getOrderById(orderId: Long): Order? = DatabaseFactory.dbQuery {
        getOrderByIdInternal(orderId)
    }

    override suspend fun updateLogistics(orderId: Long, trackId: String, company: String): Order? = DatabaseFactory.dbQuery {
        val now = Instant.now().toString()
        val updatedRows = OrdersTable.update({ OrdersTable.id eq orderId }) {
            it[OrdersTable.trackId] = trackId
            it[OrdersTable.company] = company
            it[status] = "Shipped"
            it[updatedAt] = now
        }

        if (updatedRows > 0) getOrderByIdInternal(orderId) else null
    }

    override suspend fun completeOrder(orderId: Long): Order? = DatabaseFactory.dbQuery {
        val now = Instant.now().toString()
        val updatedRows = OrdersTable.update({ OrdersTable.id eq orderId }) {
            it[status] = "Completed"
            it[updatedAt] = now
        }

        if (updatedRows > 0) getOrderByIdInternal(orderId) else null
    }

    override suspend fun updateOrderStatus(orderId: Long, status: String): Order? = DatabaseFactory.dbQuery {
        val now = Instant.now().toString()
        val updatedRows = OrdersTable.update({ OrdersTable.id eq orderId }) {
            it[OrdersTable.status] = status
            it[updatedAt] = now
        }

        if (updatedRows > 0) getOrderByIdInternal(orderId) else null
    }

    override suspend fun openRefund(
        orderId: Long,
        userId: Long,
        userName: String,
        userRole: String,
        message: String?,
        screenshots: List<String>
    ): Order? = DatabaseFactory.dbQuery {
        val order = getOrderByIdInternal(orderId) ?: return@dbQuery null
        val now = Instant.now().toString()

        OrdersTable.update({ OrdersTable.id eq orderId }) {
            it[status] = "Refund Requested"
            it[updatedAt] = now
        }

        val initialMsg = if (!message.isNullOrBlank()) message else "Refund request opened for order ${order.orderNumber.ifEmpty { order.id.toString() }}"
        addRefundMessageInternal(orderId, userId, userName, userRole, initialMsg, screenshots)

        getOrderByIdInternal(orderId)
    }

    override suspend fun addRefundMessage(
        orderId: Long,
        senderId: Long,
        senderName: String,
        senderRole: String,
        message: String,
        screenshots: List<String>
    ): RefundMessage? = DatabaseFactory.dbQuery {
        addRefundMessageInternal(orderId, senderId, senderName, senderRole, message, screenshots)
    }

    override suspend fun getRefundMessages(orderId: Long): List<RefundMessage> = DatabaseFactory.dbQuery {
        RefundMessagesTable.selectAll()
            .where { RefundMessagesTable.orderId eq orderId }
            .orderBy(RefundMessagesTable.id to SortOrder.ASC)
            .map { rowToRefundMessage(it) }
    }

    override suspend fun deleteOrder(orderId: Long): Boolean = DatabaseFactory.dbQuery {
        RefundMessagesTable.deleteWhere { RefundMessagesTable.orderId eq orderId }
        OrdersTable.deleteWhere { OrdersTable.id eq orderId } > 0
    }

    override suspend fun deleteOrderRefundMessages(orderId: Long): Boolean = DatabaseFactory.dbQuery {
        RefundMessagesTable.deleteWhere { RefundMessagesTable.orderId eq orderId } >= 0
    }

    private fun addRefundMessageInternal(
        orderId: Long,
        senderId: Long,
        senderName: String,
        senderRole: String,
        message: String,
        screenshots: List<String>
    ): RefundMessage {
        val now = Instant.now().toString()
        val jsonScreenshots = Json.encodeToString(screenshots)

        OrdersTable.update({ OrdersTable.id eq orderId }) {
            it[updatedAt] = now
        }

        val generatedId = RefundMessagesTable.insert {
            it[RefundMessagesTable.orderId] = orderId
            it[RefundMessagesTable.senderId] = senderId
            it[RefundMessagesTable.senderName] = senderName
            it[RefundMessagesTable.senderRole] = senderRole
            it[RefundMessagesTable.message] = message
            it[RefundMessagesTable.screenshots] = jsonScreenshots
            it[RefundMessagesTable.createdAt] = now
        } get RefundMessagesTable.id

        return RefundMessage(
            id = generatedId,
            orderId = orderId,
            senderId = senderId,
            senderName = senderName,
            senderRole = senderRole,
            message = message,
            screenshots = screenshots,
            createdAt = now
        )
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

        val productTitleFromDb = ProductsTable.selectAll()
            .where { ProductsTable.id eq orderProductId }
            .map { it[ProductsTable.title] }
            .singleOrNull()

        val storedProductName = row[OrdersTable.productName]
        val effectiveProductName = if (storedProductName.isNotBlank()) storedProductName else (productTitleFromDb ?: "")

        val storedProductImage = row[OrdersTable.productImage]
        val storedPaymentScreenshot = row[OrdersTable.paymentScreenshot]
        val storedPaymentImageUrl = row[OrdersTable.paymentImageUrl]
        val effectivePaymentScreenshot = if (storedPaymentScreenshot.isNotBlank()) storedPaymentScreenshot else storedPaymentImageUrl

        val storedNote = row[OrdersTable.note]
        val storedVariationNote = row[OrdersTable.variationNote]
        val effectiveNote = storedNote ?: storedVariationNote

        val storedCreatedAt = row[OrdersTable.createdAt]
        val storedUpdatedAt = try { row[OrdersTable.updatedAt] } catch (_: Exception) { "" }
        val effectiveUpdatedAt = if (storedUpdatedAt.isNotBlank()) storedUpdatedAt else storedCreatedAt

        return Order(
            id = row[OrdersTable.id],
            orderNumber = row[OrdersTable.orderNumber],
            orderDate = row[OrdersTable.orderDate],
            userId = orderUserId,
            userName = userName,
            productId = orderProductId,
            productName = effectiveProductName,
            productImage = storedProductImage,
            productSourcePrice = row[OrdersTable.productSourcePrice].toDouble(),
            productTitle = effectiveProductName,
            address = row[OrdersTable.address],
            note = effectiveNote,
            variationNote = effectiveNote,
            quantity = row[OrdersTable.quantity],
            listingPrice = row[OrdersTable.listingPrice].toDouble(),
            paymentScreenshot = effectivePaymentScreenshot,
            paymentImageUrl = effectivePaymentScreenshot,
            trackId = row[OrdersTable.trackId],
            company = row[OrdersTable.company],
            status = row[OrdersTable.status],
            createdAt = storedCreatedAt,
            updatedAt = effectiveUpdatedAt
        )
    }

    private fun rowToRefundMessage(row: ResultRow): RefundMessage {
        val rawScreenshots = row[RefundMessagesTable.screenshots]
        val screenshotsList = try {
            Json.decodeFromString<List<String>>(rawScreenshots)
        } catch (_: Exception) {
            emptyList()
        }

        return RefundMessage(
            id = row[RefundMessagesTable.id],
            orderId = row[RefundMessagesTable.orderId],
            senderId = row[RefundMessagesTable.senderId],
            senderName = row[RefundMessagesTable.senderName],
            senderRole = row[RefundMessagesTable.senderRole],
            message = row[RefundMessagesTable.message],
            screenshots = screenshotsList,
            createdAt = row[RefundMessagesTable.createdAt]
        )
    }
}
