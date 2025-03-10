package app.web.commenter_api

import app.web.commenter_api.schemas.*
import io.github.cdimascio.dotenv.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import org.jetbrains.exposed.sql.transactions.experimental.*
import java.util.*

suspend fun <T> dbQuery(block: suspend () -> T): T =
	newSuspendedTransaction(Dispatchers.IO) { block() }

fun String.toUUID() : UUID = UUID.fromString(this)

object DatabaseFactory {
	fun init() {
		val dotenv = dotenv { }
		
		val database = Database.connect(
			url = dotenv["DB_URL"],
			driver = "org.postgresql.Driver",
			user = dotenv["DB_USERNAME"],
			password = dotenv["DB_PASSWORD"]
		)
		
		transaction (database) {
			SchemaUtils.create(UsersTable)
			SchemaUtils.create(FriendsTable)
			SchemaUtils.create(FriendsUsersTable)
			SchemaUtils.create(FriendRequestTable)
			SchemaUtils.create(BlockedUsersTable)
		}
	}
}