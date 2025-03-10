package app.web.commenter_api.utils

import app.web.commenter_api.*
import io.github.cdimascio.dotenv.*
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys.hmacShaKeyFor
import java.time.*
import java.util.*

fun generateJWT(uid : String) : String {
	val now = System.currentTimeMillis()
	val dotenv = dotenv {}
	
	return Jwts
		.builder()
		.claim("uid", uid)
		.issuedAt(Date(now))
		.expiration(Date(now + Duration.ofDays(1).toMillis()))
		.signWith(hmacShaKeyFor(dotenv["JWT_SECRET"]!!.toByteArray()))
		.compact()
}

// Returns the UUID of the user from the JWT
// If the JWT has expired, will need to log in again, so return null for now
fun validateJWT(jwt : String) : UUID? {
	val dotenv = dotenv {}
	
	try {
		val uidString = Jwts
			.parser()
			.verifyWith(hmacShaKeyFor(dotenv["JWT_SECRET"]!!.toByteArray()))
			.build()
			.parseSignedClaims(jwt)
			.payload["uid"] ?: throw InvalidDetailsException("JWT is invalid")
		return uidString.toString().toUUID()
	} catch (e : ExpiredJwtException) {
		return null
	} catch (e : Exception) {
		throw e
	}
}
