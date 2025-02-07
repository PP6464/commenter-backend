package app.web.commenter_api.utils

import app.web.commenter_api.schemas.*
import kotlinx.serialization.*

@Serializable
data class EmptyResponse(
	val message : String,
	val code : Int,
)

@Serializable
data class UserResponse(
	val message : String,
	val code : Int,
	val payload : User? = null,
)