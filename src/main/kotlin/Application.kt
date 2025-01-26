@file:Suppress("unused")

package app.web.commenter_api

import app.web.commenter_api.schemas.*
import app.web.commenter_api.utils.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*

fun main(args : Array<String>) {
	io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
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
			if (cause.message!!.contains("Failed to convert request body")) {
				call.respond(
					status = HttpStatusCode.BadRequest,
					message = NoPayloadResponseBody(
						message = "Invalid body",
						code = 400,
					)
				)
				return@exception
			}
			if (cause is InvalidFieldException) {
				// A field has been deemed invalid
				call.respond(
					status = HttpStatusCode.NotAcceptable,
					message = NoPayloadResponseBody(
						message = cause.message,
						code = 406,
					),
				)
			}
			if (call.request.path().contains("sign-up")) {
				if (cause.message!!.lowercase().contains("email")) {
					call.respond(
						status = HttpStatusCode.Conflict,
						message = NoPayloadResponseBody(
							message = "Email already in use",
							code = 409,
						)
					)
					return@exception
				}
			}
			call.respond(
				status = HttpStatusCode.InternalServerError,
				message = NoPayloadResponseBody(
					code = 500,
					message = "Internal Server Error",
				)
			)
		}
	}
	
	val userDao = UserDaoImpl()
	DatabaseFactory.init()
	configureRouting(userDao = userDao)
}
