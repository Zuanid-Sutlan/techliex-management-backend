package com.techliex.presentation.routes

import com.techliex.domain.usecase.product.*
import com.techliex.presentation.dto.*
import com.techliex.security.getUserPrincipal
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.productRoutes(
    createProductUseCase: CreateProductUseCase,
    getProductsUseCase: GetProductsUseCase,
    getProductByIdUseCase: GetProductByIdUseCase,
    updateWarehouseUseCase: UpdateWarehouseUseCase,
    deleteProductUseCase: DeleteProductUseCase
) {
    route("/api/v1/products") {
        authenticate("auth-jwt") {

            // Create new product hunt
            post {
                val principal = call.getUserPrincipal()
                val req = try {
                    call.receive<CreateProductRequest>()
                } catch (_: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid payload"))
                }

                val product = createProductUseCase(
                    creatorId = principal.id,
                    title = req.title,
                    description = req.description,
                    productImageUrl = req.productImageUrl,
                    sourceLink = req.sourceLink,
                    sourcePrice = req.sourcePrice,
                    referenceLink = req.referenceLink,
                    referencePrice = req.referencePrice,
                    shareWithUsernames = req.shareWithUsernames
                )
                call.respond(HttpStatusCode.Created, ApiResponse(true, "Product hunt created successfully", product.toDto()))
            }

            // Get products visible to current user
            get {
                val principal = call.getUserPrincipal()
                val products = getProductsUseCase(principal.id, principal.role).map { it.toDto() }
                call.respond(HttpStatusCode.OK, ApiResponse(true, "Products retrieved", products))
            }

            // Get single product details
            get("/{id}") {
                val productId = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid product ID"))

                val product = getProductByIdUseCase(productId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Product not found"))

                call.respond(HttpStatusCode.OK, ApiResponse(true, "Product details retrieved", product.toDto()))
            }

            // Admin / Warehouse only: update warehouse price & note
            patch("/{id}/warehouse") {
                val principal = call.getUserPrincipal()
                if (principal.role != "Admin" && principal.role != "Warehouse") {
                    return@patch call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Admin or Warehouse role required"))
                }

                val productId = call.parameters["id"]?.toLongOrNull()
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid product ID"))

                val req = try {
                    call.receive<UpdateWarehouseRequest>()
                } catch (_: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid payload"))
                }

                val updatedProduct = updateWarehouseUseCase(productId, req.warehousePrice, req.warehouseNote)
                    ?: return@patch call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Product not found"))

                call.respond(HttpStatusCode.OK, ApiResponse(true, "Warehouse details updated", updatedProduct.toDto()))
            }

            // Admin only: delete product
            delete("/{id}") {
                val principal = call.getUserPrincipal()
                if (principal.role != "Admin") {
                    return@delete call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Admin role required"))
                }

                val productId = call.parameters["id"]?.toLongOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid product ID"))

                val deleted = deleteProductUseCase(productId)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(true, "Product deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Product not found"))
                }
            }
        }
    }
}
