package app.web.commenter_api.schemas

import app.web.commenter_api.dbQuery
import kotlinx.serialization.*
import org.jetbrains.exposed.dao.id.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.postgresql.util.PSQLException
import java.util.*

@Serializable
data class User(
	val uid : String,
	val displayName : String,
	val email : String,
	@Transient val passwordHash : String = "",
	val pic : String,
	val disabled : Boolean,
)

object Users : UUIDTable(name = "users") {
	val displayName = varchar("display_name", 20)
	val email = text("email").uniqueIndex()
	val passwordHash = text("password_hash")
	val pic = text("pic")
	val disabled = bool("disabled").default(false)
}

fun userFromRow(row : ResultRow) = User(
	uid = row[Users.id].toString(),
	displayName = row[Users.displayName].toString(),
	email = row[Users.email],
	passwordHash = row[Users.passwordHash],
	pic = row[Users.pic],
	disabled = row[Users.disabled],
)

interface UserDao {
	suspend fun allUsers() : List<User>
	suspend fun getUser(id : UUID) : User?
	suspend fun getUserByEmail(email : String) : User?
	suspend fun addNewUser(displayName : String, email : String, passwordHash : String) : User?
	suspend fun updateUser(id : UUID, displayName : String?, email : String?, passwordHash : String?) : Boolean
	suspend fun disableUser(id : UUID) : Boolean
	suspend fun enableUser(id : UUID) : Boolean
	suspend fun deleteUser(id : UUID) : Boolean
}

class UserDaoImpl : UserDao {
	override suspend fun allUsers() : List<User> = dbQuery {
		Users.selectAll().map(::userFromRow)
	}
	
	override suspend fun getUser(id : UUID) = dbQuery {
		Users.selectAll().where { Users.id eq id }.map(::userFromRow).singleOrNull()
	}
	
	override suspend fun getUserByEmail(email : String) : User? = dbQuery {
		Users.selectAll().where { Users.email eq email }.map(::userFromRow).singleOrNull()
	}
	
	override suspend fun addNewUser(displayName : String, email : String, passwordHash : String) : User? = dbQuery {
		return@dbQuery try {
			Users.insert {
				it[Users.displayName] = displayName
				it[Users.email] = email
				it[Users.passwordHash] = passwordHash
				it[pic] = "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png"
			}.resultedValues?.map(::userFromRow)?.singleOrNull()
		} catch (e: PSQLException) {
			if (e.sqlState == "23505") {
				// Email already used
				throw Exception("Email already in use")
			} else {
				throw(e)
			}
		}
	}
	
	override suspend fun updateUser(id : UUID, displayName : String?, email : String?, passwordHash : String?) : Boolean =
		dbQuery {
			val currentValues = getUser(id)!!
			
			Users.update({ Users.id eq id }) {
				it[Users.displayName] = displayName ?: currentValues.displayName
				it[Users.email] = email ?: currentValues.email
				it[Users.passwordHash] = passwordHash ?: currentValues.passwordHash
			} > 0
		}
	
	override suspend fun disableUser(id : UUID) : Boolean = dbQuery {
		Users.update({ Users.id eq id }) {
			it[disabled] = true
		} > 0
	}
	
	override suspend fun enableUser(id : UUID) : Boolean = dbQuery {
		Users.update({ Users.id eq id }) {
			it[disabled] = false
		} > 0
	}
	
	override suspend fun deleteUser(id : UUID) : Boolean = dbQuery {
		Users.deleteWhere { Users.id eq id } > 0
	}
}
