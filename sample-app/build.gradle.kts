plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.detekt)
}

detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline-sample.xml")
    buildUponDefaultConfig = true
    parallel = true
}

group = "cn.floriax.qqbot"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core"))
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.logback.classic)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.migration.jdbc)
    implementation(libs.sqlite.jdbc)
    implementation(libs.quartz)

    testImplementation(kotlin("test"))
}
