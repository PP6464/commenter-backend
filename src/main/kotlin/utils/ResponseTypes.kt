package app.web.commenter_api.utils

import app.web.commenter_api.schemas.*
import kotlinx.serialization.*

interface ResponseType {
	val message : String
	val code : Int
}

@Serializable
data class EmptyResponse(
	override val message : String,
	override val code : Int,
) : ResponseType

@Serializable
data class UserResponse(
	override val message : String,
	override val code : Int,
	val payload : User? = null,
) : ResponseType

@Serializable
data class FriendsResponse(
	val friends : List<User>,
	override val code : Int,
	override val message : String,
) : ResponseType

@Serializable
data class FriendRequestsResponse(
	override val message : String,
	override val code : Int,
	val from : List<User>,
) : ResponseType
