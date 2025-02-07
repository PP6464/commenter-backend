plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.ktor)
	alias(libs.plugins.kotlin.plugin.serialization)
}

group = "app.web.commenter_api"
version = "0.0.1"

application {
	mainClass.set("io.ktor.server.netty.EngineMain")
	
	val isDevelopment : Boolean = project.ext.has("development")
	applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.ktor.server.core)
	implementation(libs.ktor.serialization.kotlinx.json)
	implementation(libs.ktor.server.content.negotiation)
	implementation(libs.ktor.server.config.yaml)
	implementation(libs.ktor.server.netty)
	implementation(libs.ktor.cors)
	implementation(libs.kotlinx.serialization.json)
	implementation(libs.logback.classic)
	implementation(libs.postgresql)
	implementation(libs.h2)
	implementation(libs.argon2.jvm)
	implementation(libs.dotenv)
	implementation(libs.exposed.core)
	implementation(libs.exposed.dao)
	implementation(libs.exposed.jdbc)
	implementation(libs.exposed.datetime)
	implementation(libs.jjwt.api)
	implementation(libs.jjwt.impl)
	implementation(libs.jjwt.jackson)
	implementation(libs.status.pages)
	implementation(libs.ktor.logging)
	implementation(libs.firebase.admin)
	implementation(libs.koin)
	testImplementation(libs.ktor.server.test.host)
	testImplementation(libs.kotlin.test.junit)
	implementation(kotlin("test"))
}
