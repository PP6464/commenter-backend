package app.web.commenter_api

import app.web.commenter_api.schemas.*
import app.web.commenter_api.utils.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(userDao : UserDao) {
	routing {
		get("/") {
			call.respondText("Hello World!")
		}
		
		post("/sign-up") {
			val userData = call.receive<SignUpBody>()
			
			if (userData.password.length < 10) {
				throw InvalidFieldException("Password is too short")
			}
			
			if (userData.displayName.length > 20) {
				throw InvalidFieldException("Display name is too long")
			}
			
			val emailRegex = Regex("[-\\w.]+@[\\w-]+\\.[\\w-]+")
			
			if (!emailRegex.matches(userData.email)) {
				throw InvalidFieldException("Email is invalid")
			}
			
			val passwordHash = hashPassword(userData.password)
			
			val user = userDao.addNewUser(userData.displayName, userData.email, passwordHash)!!
				
			val jwt = generateJWT(user.uid)
			
			call.response.cookies.append("jwt", jwt, maxAge = 86400, httpOnly = true, secure = true, path = "/")
			call.respond(
				status = HttpStatusCode.Created,
				message = UserResponseBody(
					message = "Created user successfully",
					code = 201,
					payload = user
				)
			)
		}
	}
}
