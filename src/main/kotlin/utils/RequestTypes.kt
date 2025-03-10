package app.web.commenter_api.utils

import kotlinx.serialization.Serializable

@Serializable
data class SignUpBody(
	val email: String,
	val password: String,
	val displayName : String,
)

@Serializable
data class LoginBody(
	val email: String,
	val password: String,
)

@Serializable
data class ProfileUpdateBody(
	val uid : String,
	val displayName : String? = null,
	val email : String? = null,
	val password : String? = null,
	val pic : String? = null,
	val status: String? = null,
	val hasPicFile : Boolean = false
)

@Serializable
data class FriendRequestBody(
	val from : String,
	val to : String,
)

@Serializable
data class BlockUserBody(
	val from : String,
	val to : String,
)

@Serializable
data class DeleteFriendBody(
	val user1 : String,
	val user2 : String,
)