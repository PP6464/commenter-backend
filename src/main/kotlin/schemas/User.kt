package app.web.commenter_api.schemas

import app.web.commenter_api.*
import app.web.commenter_api.utils.*
import kotlinx.serialization.*
import org.jetbrains.exposed.dao.id.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.postgresql.util.*
import java.util.*

@Serializable
data class User(
	val uid : String,
	val displayName : String,
	val email : String,
	@Transient val passwordHash : String = "",
	val pic : String,
	val status : String,
	val disabled : Boolean,
)

object UsersTable : UUIDTable(name = "users") {
	val displayName = varchar("display_name", 20)
	val email = text("email").uniqueIndex()
	val passwordHash = text("password_hash")
	val pic = text("pic")
	val disabled = bool("disabled").default(false)
	val status = varchar("status", 50)
}

fun userFromRow(row : ResultRow) = User(
	uid = row[UsersTable.id].toString(),
	displayName = row[UsersTable.displayName].toString(),
	email = row[UsersTable.email],
	passwordHash = row[UsersTable.passwordHash],
	pic = row[UsersTable.pic],
	disabled = row[UsersTable.disabled],
	status = row[UsersTable.status],
)

interface UserDao {
	suspend fun allUsers() : List<User>
	suspend fun getUser(id : UUID) : User?
	suspend fun getUserByEmail(email : String) : User?
	suspend fun addNewUser(displayName : String, email : String, passwordHash : String) : User?
	suspend fun updateUser(
		id : UUID,
		displayName : String? = null,
		email : String? = null,
		passwordHash : String? = null,
		pic : String? = null,
		status : String? = null
	) : Boolean
	
	suspend fun disableUser(id : UUID) : Boolean
	suspend fun enableUser(id : UUID) : Boolean
	suspend fun deleteUser(id : UUID) : Boolean
}

class UserDaoImpl : UserDao {
	private val blankPicURL = "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png"
	
	override suspend fun allUsers() : List<User> = dbQuery {
		UsersTable.selectAll().map(::userFromRow)
	}
	
	override suspend fun getUser(id : UUID) = dbQuery {
		UsersTable.selectAll().where { UsersTable.id eq id }.map(::userFromRow).singleOrNull()
	}
	
	override suspend fun getUserByEmail(email : String) : User? = dbQuery {
		UsersTable.selectAll().where { UsersTable.email eq email }.map(::userFromRow).singleOrNull()
	}
	
	override suspend fun addNewUser(displayName : String, email : String, passwordHash : String) : User? = dbQuery {
		return@dbQuery try {
			UsersTable.insert {
				it[UsersTable.displayName] = displayName
				it[UsersTable.email] = email
				it[UsersTable.passwordHash] = passwordHash
				it[pic] = blankPicURL
			}.resultedValues?.map(::userFromRow)?.singleOrNull()
		} catch (e : PSQLException) {
			if (e.sqlState == "23505") {
				// Email already used
				throw ConflictException("Email already in use")
			} else {
				throw (e)
			}
		}
	}
	
	override suspend fun updateUser(
		id : UUID,
		displayName : String?,
		email : String?,
		passwordHash : String?,
		pic : String?,
		status : String?
	) : Boolean =
		dbQuery {
			val currentValues = getUser(id)!!
			
			if (displayName?.length?.let { it > 20 } == true) throw InvalidDetailsException("Display name too long")
			if (displayName?.isEmpty() == true) throw InvalidDetailsException("Display name cannot be empty")
			if (status?.let { it.length > 50 } == true) throw InvalidDetailsException("Status is too long")
			
			UsersTable.update({ UsersTable.id eq id }) {
				it[UsersTable.displayName] = displayName ?: currentValues.displayName
				it[UsersTable.email] = email ?: currentValues.email
				it[UsersTable.passwordHash] = passwordHash ?: currentValues.passwordHash
				it[UsersTable.pic] = pic ?: currentValues.pic
				it[UsersTable.status] = status ?: currentValues.status
			} > 0
		}
	
	override suspend fun disableUser(id : UUID) : Boolean = dbQuery {
		UsersTable.update({ UsersTable.id eq id }) {
			it[disabled] = true
		} > 0
	}
	
	override suspend fun enableUser(id : UUID) : Boolean = dbQuery {
		UsersTable.update({ UsersTable.id eq id }) {
			it[disabled] = false
		} > 0
	}
	
	override suspend fun deleteUser(id : UUID) : Boolean = dbQuery {
		UsersTable.deleteWhere { UsersTable.id eq id } > 0
	}
}
