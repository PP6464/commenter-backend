package app.web.commenter_api

import app.web.commenter_api.schemas.*
import app.web.commenter_api.utils.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
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
			
			if (userData.displayName.isEmpty()) {
				throw InvalidFieldException("Display name cannot be empty")
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
		
		post("/login") {
			val userData = call.receive<LoginBody>()
			
			val user = userDao.getUserByEmail(userData.email) ?: throw NotFoundException("User not found")
			
			if (!verifyPassword(userData.password, user.passwordHash)) {
				throw InvalidDetailsException("Incorrect password")
			}
			
			if (user.disabled) {
				throw InvalidDetailsException("This account is disabled")
			}
			
			val jwt = generateJWT(user.uid)
			
			call.response.cookies.append(
				name = "jwt",
				value = jwt,
				path = "/",
				httpOnly = true,
				secure = true,
				maxAge = 86400,
			)
			
			call.respond(
				status = HttpStatusCode.OK,
				message = UserResponseBody(
					message = "User logged in successfully",
					code = 200,
					payload = user,
				),
			)
		}
		
		get("/re-auth") {
			val jwt = call.request.cookies["jwt"] ?: throw InvalidDetailsException("JWT is missing")
			val uid = validateJWT(jwt) ?: run {
				println("Expired")
				throw InvalidDetailsException("JWT is expired")
			}
			
			val user = userDao.getUser(uid) ?: throw NotFoundException("The user is not found")
			
			call.respond(
				status = HttpStatusCode.OK,
				message = UserResponseBody(
					message = "Re-authenticated successfully",
					code = 200,
					payload = user,
				),
			)
		}
		
		post("/logout") {
			call.response.cookies.append(
				name = "jwt",
				value = "",
				path = "/",
				httpOnly = true,
				secure = true,
				maxAge = 0,
			)
			
			call.respond(
				status = HttpStatusCode.OK,
				message = NoPayloadResponseBody(
					message = "Logged out successfully",
					code = 200,
				)
			)
		}
	}
}
