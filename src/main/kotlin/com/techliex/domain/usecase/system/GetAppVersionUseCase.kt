package com.techliex.domain.usecase.system

import com.techliex.domain.model.AppVersion
import java.io.File
import java.util.Properties

class GetAppVersionUseCase {
    operator fun invoke(): AppVersion {
        val properties = Properties()
        
        // 1. Try reading version.properties from ClassLoader resources
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("version.properties")
        if (stream != null) {
            properties.load(stream)
        }
        
        // 2. Direct check on gradle.properties (enables instant live update when gradle.properties is edited)
        val gradlePropsFile = File("gradle.properties")
        val gradleProps = Properties()
        if (gradlePropsFile.exists()) {
            try {
                gradlePropsFile.inputStream().use { gradleProps.load(it) }
            } catch (_: Exception) {}
        }

        val major = (gradleProps.getProperty("app_major") ?: properties.getProperty("app_major"))?.toIntOrNull() ?: 0
        val minor = (gradleProps.getProperty("app_minor") ?: properties.getProperty("app_minor"))?.toIntOrNull() ?: 0
        val patch = (gradleProps.getProperty("app_patch") ?: properties.getProperty("app_patch"))?.toIntOrNull() ?: 0
        val stage = (gradleProps.getProperty("app_prerelease_stage") ?: properties.getProperty("app_prerelease_stage") ?: "").trim()
        val stageNumber = (gradleProps.getProperty("app_prerelease_number") ?: properties.getProperty("app_prerelease_number"))?.toIntOrNull() ?: 0
        val appName = gradleProps.getProperty("app_name") ?: properties.getProperty("app.name", "Techliex Management")

        val calculatedCode = if (stage.isNotBlank()) {
            (major * 10000 + minor * 1000 + patch * 10 + stageNumber)
        } else {
            (major * 10000 + minor * 1000 + patch * 10)
        }
        val versionCode = maxOf(1, if (gradlePropsFile.exists()) calculatedCode else properties.getProperty("version.code")?.toIntOrNull() ?: calculatedCode)

        val calculatedName = if (stage.isNotBlank()) {
            "$major.$minor.$patch-$stage$stageNumber"
        } else {
            "$major.$minor.$patch"
        }
        val versionName = if (gradlePropsFile.exists()) calculatedName else properties.getProperty("version.name", calculatedName)

        return AppVersion(
            versionName = versionName,
            versionCode = versionCode,
            name = appName
        )
    }
}
