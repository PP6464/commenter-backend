package app.web.commenter_api.utils

class InvalidFieldException(override val message: String) : Exception()

class InvalidDetailsException(override val message: String) : Exception()

class InsufficientPermissionException(override val message : String) : Exception()

class PayloadSizeException(override val message : String) : Exception()

class InvalidMediaException(override val message : String) : Exception()