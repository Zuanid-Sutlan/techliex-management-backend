
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    kotlin("plugin.serialization") version "2.1.0"
}

group = "com.techliex"
version = "1.0.0"

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
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-server-openapi:$ktorVersion")
    implementation(libs.logback.classic)

    // Database & Security
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("at.favre.lib:bcrypt:$bcryptVersion")

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
