package app.web.commenter_api

import app.web.commenter_api.schemas.*
import app.web.commenter_api.storage.*
import app.web.commenter_api.utils.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.*
import org.koin.ktor.ext.*
import kotlin.test.*

fun Application.configureRouting(userDao : UserDao) {
	val storageService by inject<StorageService>()
	
	routing {
		get("/") {
			call.respondText("Hello World!")
		}
		
		// Profile
		post("/update-profile") {
			val jwt = call.request.cookies["jwt"] ?: throw InvalidDetailsException("JWT is missing")
			val uid = validateJWT(jwt) ?: throw InvalidDetailsException("JWT is expired")
			val user = userDao.getUser(uid) ?: throw NotFoundException("The user is not found")
			
			if (user.disabled) {
				call.respond(
					status = HttpStatusCode.Forbidden,
					message = EmptyResponse(
						message = "This account is disabled",
						code = 403,
					),
				)
				return@post
			}
			
			val multipart = call.receiveMultipart()
			// File
			var file : ByteArray? = null
			var extension : String? = null
			var contentType : String? = null
			val allowedMimeTypes = listOf("image/png", "image/jpeg", "image/webp", "image/avif", "image/tiff")
			// Profile
			var updateInfo : ProfileUpdateBody? = null
			
			multipart.forEachPart { part ->
				when (part) {
					is PartData.FileItem -> {
						assertEquals("file", part.name, "406: The file part has been incorrectly named")
						file = part.provider().toByteArray()
						contentType = part.contentType?.toString() ?: throw InvalidDetailsException("File is missing but file body part declared")
						if (contentType !in allowedMimeTypes) throw InvalidMediaException("Unsupported file type")
						extension = part.originalFileName?.substringAfterLast(".", "") ?: throw InvalidDetailsException("File has no name")
					}
					is PartData.FormItem -> {
						assertEquals("info", part.name, "406: The json body part has been incorrectly named")
						updateInfo = Json.decodeFromString<ProfileUpdateBody>(part.value)
						if (updateInfo!!.uid != user.uid) {
							throw ConflictException("UIDs supplied do not match")
						}
					}
					else -> {}
				}
				
				part.dispose
			}
			
			val updated : Boolean
			
			if (updateInfo!!.hasPicFile) {
				assertNotEquals(null, file, "406: Picture declared in json body but not supplied")
				
				if (file!!.size > 2 * 1024 * 1024) {
					throw PayloadSizeException("Profile picture must not exceed 2MB in size")
				}
				
				storageService.deleteDir("users/$uid")
				val url = storageService.upload("users/$uid/profile-pic.${extension!!}", file!!, contentType!!)
				updated = userDao.updateUser(
					id = uid,
					displayName = updateInfo!!.displayName,
					email = updateInfo!!.email,
					passwordHash = updateInfo!!.password?.let { hashPassword(it) },
					pic = url,
					status = updateInfo!!.status,
				)
			} else {
				updated = userDao.updateUser(
					id = uid,
					displayName = updateInfo!!.displayName,
					email = updateInfo!!.email,
					passwordHash = updateInfo!!.password?.let { hashPassword(it) },
					pic = updateInfo!!.pic,
					status = updateInfo!!.status,
				)
			}
			
			if (updated) {
				call.respond(
					status = HttpStatusCode.OK,
					message = UserResponse(
						code = 200,
						message = "Profile successfully updated",
						payload = userDao.getUser(uid),
					),
				)
			} else {
				call.respond(
					status = HttpStatusCode.InternalServerError,
					message = EmptyResponse(
						code = 500,
						message = "Profile did not update"
					)
				)
			}
		}
		
		// Auth
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
				message = UserResponse(
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
				message = UserResponse(
					message = "User logged in successfully",
					code = 200,
					payload = user,
				),
			)
		}
		
		get("/re-auth") {
			val jwt = call.request.cookies["jwt"] ?: throw InvalidDetailsException("JWT is missing")
			val uid = validateJWT(jwt) ?: throw InvalidDetailsException("JWT is expired")
			val user = userDao.getUser(uid) ?: throw NotFoundException("The user is not found")
			
			call.respond(
				status = HttpStatusCode.OK,
				message = UserResponse(
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
				message = EmptyResponse(
					message = "Logged out successfully",
					code = 200,
				)
			)
		}
	}
}
