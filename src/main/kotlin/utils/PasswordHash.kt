package app.web.commenter_api.utils

import de.mkammerer.argon2.*


fun hashPassword(password : String) : String {
	val argon2 = Argon2Factory.create()
	
	return argon2.hash(1, 65536, 1, password.toByteArray())
}

fun verifyPassword(password : String, hash : String) : Boolean {
	val argon2 = Argon2Factory.create()
	
	return argon2.verify(hash, password.toByteArray())
}