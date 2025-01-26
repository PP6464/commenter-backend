package app.web.commenter_api

import app.web.commenter_api.schemas.*
import io.github.cdimascio.dotenv.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import org.jetbrains.exposed.sql.transactions.experimental.*

suspend fun <T> dbQuery(block: suspend () -> T): T =
	newSuspendedTransaction(Dispatchers.IO) { block() }

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
			SchemaUtils.create(Users)
		}
	}
}