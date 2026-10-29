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
    updateProductUseCase: UpdateProductUseCase,
    updateWarehouseUseCase: UpdateWarehouseUseCase,
    updateProductSharesUseCase: UpdateProductSharesUseCase,
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
                    sharedUserIds = req.sharedUserIds,
                    shareWithUsernames = req.shareWithUsernames
                )
                call.respond(HttpStatusCode.Created, ApiResponse(true, "Product hunt created successfully", product.toDto()))
            }

            // Get products visible to current user (Admin sees all; Member sees owned or shared)
            get {
                val principal = call.getUserPrincipal()
                val products = getProductsUseCase(principal.id, principal.role).map { it.toDto() }
                call.respond(HttpStatusCode.OK, ApiResponse(true, "Products retrieved", products))
            }

            // Get single product details (with visibility enforcement)
            get("/{id}") {
                val principal = call.getUserPrincipal()
                val productId = call.parameters["id"]?.toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid product ID"))

                val product = getProductByIdUseCase(productId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Product not found"))

                // Enforce visibility for non-admin/non-warehouse users
                if (principal.role != "Admin" && principal.role != "Warehouse") {
                    val isOwner = product.userId == principal.id
                    val isShared = principal.id in product.sharedUserIds || principal.username in product.sharedUsernames
                    if (!isOwner && !isShared) {
                        return@get call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Access denied: Product not shared with you"))
                    }
                }

                call.respond(HttpStatusCode.OK, ApiResponse(true, "Product details retrieved", product.toDto()))
            }

            // Update product details (Admin or Product Creator)
            patch("/{id}") {
                val principal = call.getUserPrincipal()
                val productId = call.parameters["id"]?.toLongOrNull()
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid product ID"))

                val existingProduct = getProductByIdUseCase(productId)
                    ?: return@patch call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Product not found"))

                if (principal.role != "Admin" && existingProduct.userId != principal.id) {
                    return@patch call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Only Admin or product creator can update product"))
                }

                val req = try {
                    call.receive<UpdateProductRequest>()
                } catch (_: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid payload"))
                }

                val updatedProduct = updateProductUseCase(
                    productId = productId,
                    title = req.title,
                    description = req.description,
                    productImageUrl = req.productImageUrl,
                    sourceLink = req.sourceLink,
                    sourcePrice = req.sourcePrice,
                    referenceLink = req.referenceLink,
                    referencePrice = req.referencePrice,
                    sharedUserIds = req.sharedUserIds,
                    shareWithUsernames = req.shareWithUsernames
                ) ?: return@patch call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Product not found"))

                call.respond(HttpStatusCode.OK, ApiResponse(true, "Product updated successfully", updatedProduct.toDto()))
            }

            // Update product shares / visible members (Admin or Product Creator)
            patch("/{id}/shares") {
                val principal = call.getUserPrincipal()
                val productId = call.parameters["id"]?.toLongOrNull()
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid product ID"))

                val existingProduct = getProductByIdUseCase(productId)
                    ?: return@patch call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Product not found"))

                if (principal.role != "Admin" && existingProduct.userId != principal.id) {
                    return@patch call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Only Admin or product creator can update shares"))
                }

                val req = try {
                    call.receive<UpdateProductSharesRequest>()
                } catch (_: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid payload"))
                }

                val updatedProduct = updateProductSharesUseCase(
                    productId = productId,
                    sharedUserIds = req.sharedUserIds,
                    shareWithUsernames = req.shareWithUsernames
                ) ?: return@patch call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Product not found"))

                call.respond(HttpStatusCode.OK, ApiResponse(true, "Product shares updated successfully", updatedProduct.toDto()))
            }

            // Update source and reference links (Admin, Warehouse, Creator, or Shared Members)
            patch("/{id}/links") {
                val principal = call.getUserPrincipal()
                val productId = call.parameters["id"]?.toLongOrNull()
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid product ID"))

                val existingProduct = getProductByIdUseCase(productId)
                    ?: return@patch call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Product not found"))

                if (principal.role != "Admin" && principal.role != "Warehouse") {
                    val isOwner = existingProduct.userId == principal.id
                    val isShared = principal.id in existingProduct.sharedUserIds || principal.username in existingProduct.sharedUsernames
                    if (!isOwner && !isShared) {
                        return@patch call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, "Access denied: Product not shared with you"))
                    }
                }

                val req = try {
                    call.receive<UpdateProductLinksRequest>()
                } catch (_: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid payload"))
                }

                val updatedProduct = updateProductUseCase(
                    productId = productId,
                    sourceLink = req.sourceLink,
                    sourcePrice = req.sourcePrice,
                    referenceLink = req.referenceLink,
                    referencePrice = req.referencePrice
                ) ?: return@patch call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Product not found"))

                call.respond(HttpStatusCode.OK, ApiResponse(true, "Product links updated successfully", updatedProduct.toDto()))
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

                val effectiveWarehouseImage = req.warehouseImageUrl ?: req.warehouseImage
                val updatedProduct = updateWarehouseUseCase(
                    productId = productId,
                    warehousePrice = req.warehousePrice,
                    warehouseNote = req.warehouseNote,
                    warehouseImageUrl = effectiveWarehouseImage
                ) ?: return@patch call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, "Product not found"))

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
