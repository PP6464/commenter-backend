package app.web.commenter_api.utils

import app.web.commenter_api.schemas.*
import kotlinx.serialization.*

class InvalidFieldException(override val message: String) : Exception()

@Serializable
data class NoPayloadResponseBody(
	val message : String,
	val code : Int,
)

@Serializable
data class UserResponseBody(
	val message : String,
	val code : Int,
	val payload : User? = null,
)