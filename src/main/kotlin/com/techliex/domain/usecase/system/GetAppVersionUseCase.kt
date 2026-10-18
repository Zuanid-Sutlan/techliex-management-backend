package com.techliex.domain.usecase.system

import com.techliex.domain.model.AppVersion
import java.util.Properties

class GetAppVersionUseCase {
    operator fun invoke(): AppVersion {
        val properties = Properties()
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("version.properties")
        if (stream != null) {
            properties.load(stream)
        }
        val versionName = properties.getProperty("version.name", "1.0.0")
        val versionCode = properties.getProperty("version.code", "1").toIntOrNull() ?: 1
        val appName = properties.getProperty("app.name", "Techliex Management")

        return AppVersion(
            versionName = versionName,
            versionCode = versionCode,
            name = appName
        )
    }
}
