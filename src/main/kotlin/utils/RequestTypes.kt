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
data class UploadRequest(
	val path : String,
)