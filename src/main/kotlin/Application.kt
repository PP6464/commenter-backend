@file:Suppress("unused")

package app.web.commenter_api

import app.web.commenter_api.schemas.*
import app.web.commenter_api.storage.*
import app.web.commenter_api.utils.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.koin.core.module.dsl.*
import org.koin.dsl.*
import org.koin.ktor.plugin.*
import org.slf4j.event.*

fun main(args : Array<String>) {
	io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
	install(CallLogging) {
		level = Level.INFO
		format { call ->
			val status = call.response.status()!!.value
			val path = call.request.path()
			val method = call.request.httpMethod
			return@format "$status $path $method"
		}
	}
	
	install(CORS) {
		allowSameOrigin = true
		allowCredentials = true
		allowNonSimpleContentTypes = true
		allowHost("localhost", schemes = listOf("https", "http")) // Allow localhost
		allowHost("commenter--chat.web.app", schemes = listOf("https"))
		allowHost("commenter--chat.firebaseapp.com", schemes = listOf("https"))
		methods.addAll(setOf(HttpMethod.Get, HttpMethod.Post, HttpMethod.Delete))
	}
	
	install(ContentNegotiation) {
		json()
	}
	
	install(StatusPages) {
		exception<Throwable> { call, cause ->
			println("ERROR - PATH: ${call.request.path()}, METHOD: ${call.request.httpMethod}, CAUSE: ${cause.message}")
			if (cause.message!!.contains("Failed to convert request body")) {
				call.respond(
					status = HttpStatusCode.BadRequest,
					message = EmptyResponse(
						message = "Invalid body",
						code = 400,
					)
				)
				return@exception
			}
			if (cause is InvalidFieldException) {
				call.respond(
					status = HttpStatusCode.NotAcceptable,
					message = EmptyResponse(
						message = cause.message,
						code = 406,
					),
				)
			}
			if (cause is ConflictException) {
				call.respond(
					status = HttpStatusCode.Conflict,
					message = EmptyResponse(
						message = cause.message,
						code = 409,
					)
				)
				return@exception
			}
			if (cause is NotFoundException) {
				call.respond(
					status = HttpStatusCode.NotFound,
					message = EmptyResponse(
						code = 404,
						message = cause.message!!,
					),
				)
			}
			if (cause is InvalidDetailsException) {
				call.respond(
					status = HttpStatusCode.NotAcceptable,
					message = EmptyResponse(
						message = cause.message,
						code = 406,
					),
				)
			}
			if (cause is InsufficientPermissionException) {
				call.respond(
					status = HttpStatusCode.Forbidden,
					message = EmptyResponse(
						message = cause.message,
						code = 403,
					),
				)
			}
			if (cause is PayloadSizeException) {
				call.respond(
					status = HttpStatusCode.PayloadTooLarge,
					message = EmptyResponse(
						message = cause.message,
						code = 413,
					),
				)
			}
			if (cause is InvalidMediaException) {
				call.respond(
					status = HttpStatusCode.UnsupportedMediaType,
					message = EmptyResponse(
						message = cause.message,
						code = 415,
					)
				)
			}
			if (cause is AssertionError) {
				call.respond(
					status = HttpStatusCode.allStatusCodes.single { it.value == cause.message!!.split(": ")[0].toInt() },
					message = EmptyResponse(
						message = cause.message!!.split(": ")[1],
						code = cause.message!!.split(": ")[0].toInt(),
					),
				)
			}
			call.respond(
				status = HttpStatusCode.InternalServerError,
				message = EmptyResponse(
					code = 500,
					message = "Internal Server Error",
				)
			)
		}
	}
	
	val appModule = module {
		singleOf(::StorageService)
	}
	
	install(Koin) {
		modules(appModule)
	}
	
	val userDao = UserDaoImpl()
	DatabaseFactory.init()
	configureRouting(userDao = userDao)
}
