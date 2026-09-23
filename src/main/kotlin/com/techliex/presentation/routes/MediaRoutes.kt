package com.techliex.presentation.routes

import com.techliex.presentation.dto.ApiResponse
import com.techliex.presentation.dto.UploadResponse
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.util.UUID

fun Route.mediaRoutes(uploadDirName: String = "uploads") {
    val uploadDir = File(uploadDirName)
    if (!uploadDir.exists()) {
        uploadDir.mkdirs()
    }

    staticFiles("/uploads", uploadDir)

    route("/api/v1/media") {
        authenticate("auth-jwt") {
            post("/upload") {
                val multipart = try {
                    call.receiveMultipart()
                } catch (_: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "Invalid multipart payload"))
                }

                var uploadedFileName: String? = null

                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        val originalFileName = part.originalFileName ?: "file.jpg"
                        val extension = originalFileName.substringAfterLast('.', "jpg")
                        val fileName = "${UUID.randomUUID()}.$extension"
                        val file = File(uploadDir, fileName)

                        @Suppress("DEPRECATION")
                        part.streamProvider().use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        uploadedFileName = fileName
                    }
                    part.dispose()
                }

                if (uploadedFileName != null) {
                    val imageUrl = "/uploads/$uploadedFileName"
                    call.respond(HttpStatusCode.OK, ApiResponse(true, "Image uploaded successfully", UploadResponse(imageUrl)))
                } else {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, "No file uploaded"))
                }
            }
        }
    }
}
