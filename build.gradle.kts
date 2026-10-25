plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    kotlin("plugin.serialization") version "2.1.0"
}

fun getVersionCode(): Int {
    val major = (project.findProperty("app_major") as? String)?.toIntOrNull() ?: 0
    val minor = (project.findProperty("app_minor") as? String)?.toIntOrNull() ?: 0
    val patch = (project.findProperty("app_patch") as? String)?.toIntOrNull() ?: 0
    val stage = (project.findProperty("app_prerelease_stage") ?: "").toString()
    val stageNumber = (project.findProperty("app_prerelease_number") as? String)?.toIntOrNull() ?: 0

    val calculated = if (stage.isNotBlank()) {
        (major * 10000 + minor * 1000 + patch * 10 + stageNumber)
    } else {
        (major * 10000 + minor * 1000 + patch * 10)
    }
    return maxOf(1, calculated)
}

fun getVersionName(): String {
    val major = project.findProperty("app_major") ?: "0"
    val minor = project.findProperty("app_minor") ?: "0"
    val patch = project.findProperty("app_patch") ?: "0"
    val stage = (project.findProperty("app_prerelease_stage") ?: "").toString()
    val stageNumber = (project.findProperty("app_prerelease_number") ?: "").toString()

    return if (stage.isNotBlank()) "$major.$minor.$patch-$stage$stageNumber"
    else "$major.$minor.$patch"
}

group = "com.techliex"
version = getVersionName()

val generateVersionProperties = tasks.register("generateVersionProperties") {
    inputs.file(rootProject.file("gradle.properties"))
    val outputDir = layout.buildDirectory.dir("generated/resources").get().asFile
    outputs.dir(outputDir)
    doLast {
        val file = File(outputDir, "version.properties")
        file.parentFile.mkdirs()
        file.writeText(
            """
            version.name=${getVersionName()}
            version.code=${getVersionCode()}
            app.name=${project.findProperty("app_name") ?: "Techliex Management"}
            """.trimIndent()
        )
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateVersionProperties)
}

sourceSets {
    main {
        resources {
            srcDir(generateVersionProperties)
        }
    }
}

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

val ktorVersion = "3.0.3"
val exposedVersion = "0.57.0"
val hikariVersion = "5.1.0"
val postgresVersion = "42.7.4"
val bcryptVersion = "0.10.2"

dependencies {
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-server-openapi:$ktorVersion")
    implementation(libs.logback.classic)
    implementation("org.slf4j:slf4j-api:2.0.16")

    // Database & Security
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.h2database:h2:2.3.232")
    implementation("at.favre.lib:bcrypt:$bcryptVersion")

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
}
