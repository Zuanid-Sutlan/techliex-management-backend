package com.techliex.plugins

import com.techliex.data.repository.*
import com.techliex.domain.usecase.auth.*
import com.techliex.domain.usecase.dashboard.*
import com.techliex.domain.usecase.order.*
import com.techliex.domain.usecase.product.*
import com.techliex.domain.usecase.user.*
import com.techliex.presentation.routes.*
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    // Instantiate Repositories (Data Layer)
    val userRepository = UserRepositoryImpl()
    val productRepository = ProductRepositoryImpl()
    val orderRepository = OrderRepositoryImpl()
    val dashboardRepository = DashboardRepositoryImpl()

    // Instantiate Use Cases (Domain Layer)
    val loginUseCase = LoginUseCase(userRepository)
    val getUserProfileUseCase = GetUserProfileUseCase(userRepository)

    val createUserUseCase = CreateUserUseCase(userRepository)
    val getAllUsersUseCase = GetAllUsersUseCase(userRepository)
    val getUserByUsernameUseCase = GetUserByUsernameUseCase(userRepository)
    val deactivateUserUseCase = DeactivateUserUseCase(userRepository)

    val createProductUseCase = CreateProductUseCase(productRepository)
    val getProductsUseCase = GetProductsUseCase(productRepository)
    val getProductByIdUseCase = GetProductByIdUseCase(productRepository)
    val updateWarehouseUseCase = UpdateWarehouseUseCase(productRepository)
    val deleteProductUseCase = DeleteProductUseCase(productRepository)

    val createOrderUseCase = CreateOrderUseCase(orderRepository)
    val getOrdersUseCase = GetOrdersUseCase(orderRepository)
    val getOrderByIdUseCase = GetOrderByIdUseCase(orderRepository)
    val updateLogisticsUseCase = UpdateLogisticsUseCase(orderRepository)
    val completeOrderUseCase = CompleteOrderUseCase(orderRepository)

    val getDashboardStatsUseCase = GetDashboardStatsUseCase(dashboardRepository)

    routing {
        get("/") {
            call.respondText("Techliex Management API Service is running!")
        }

        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")

        authRoutes(loginUseCase, getUserProfileUseCase)
        userRoutes(createUserUseCase, getAllUsersUseCase, getUserByUsernameUseCase, deactivateUserUseCase)
        productRoutes(createProductUseCase, getProductsUseCase, getProductByIdUseCase, updateWarehouseUseCase, deleteProductUseCase)
        orderRoutes(createOrderUseCase, getOrdersUseCase, getOrderByIdUseCase, updateLogisticsUseCase, completeOrderUseCase)
        dashboardRoutes(getDashboardStatsUseCase)
        mediaRoutes()
    }
}
