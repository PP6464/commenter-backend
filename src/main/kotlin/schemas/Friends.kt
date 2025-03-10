package app.web.commenter_api.schemas

import app.web.commenter_api.*
import kotlinx.serialization.*
import org.jetbrains.exposed.dao.id.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList

@Serializable
data class FriendRequest(
	val id : Long,
	val fromId : String,
	val toId : String,
)

@Serializable
data class Friends(
	val id : Long,
)

object FriendsTable : LongIdTable(name = "friends")

object FriendsUsersTable : Table(name = "friends_users") {
	val user = reference("user", UsersTable, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)
	val friend = reference("friend", FriendsTable, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)
	
	init {
		uniqueIndex("uq_friends_users_records", user, friend)
	}
}

object FriendRequestTable : LongIdTable(name = "friend_requests") {
	val fromId = reference("from", UsersTable, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)
	val toId = reference("to", UsersTable, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)
	
	init {
		uniqueIndex("uq_friend_requests", fromId, toId)
	}
}

object BlockedUsersTable : Table(name = "blocked_users") {
	val blockFrom = reference("block_from", UsersTable, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)
	val blockTo = reference("block_to", UsersTable, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)
	
	init {
		uniqueIndex("uq_blocked_users", blockFrom, blockTo)
	}
}

fun friendRequestFromRow(row : ResultRow) = FriendRequest(
	id = row[FriendRequestTable.id].value,
	fromId = row[FriendRequestTable.fromId].toString(),
	toId = row[FriendRequestTable.toId].toString(),
)

interface FriendsDao {
	suspend fun getFriendsOfUser(userId : String) : List<User>
	suspend fun deleteFriendship(user1 : String, user2 : String) : Boolean
	suspend fun addNewFriends(user1 : String, user2 : String) : Friends
}

class FriendsDaoImpl : FriendsDao {
	override suspend fun getFriendsOfUser(userId : String) : List<User> = dbQuery {
		val friends = FriendsUsersTable.selectAll().where { FriendsUsersTable.user eq userId.toUUID() }
			.map { it[FriendsUsersTable.friend].value }
		val userIds = FriendsUsersTable.selectAll()
			.where { (FriendsUsersTable.friend inList friends) and (FriendsUsersTable.user neq userId.toUUID()) }
			.map { it[FriendsUsersTable.user] }
		UsersTable.selectAll().where { UsersTable.id inList userIds }.map(::userFromRow)
	}
	
	override suspend fun deleteFriendship(user1 : String, user2 : String) : Boolean = dbQuery {
		val possibleFriends = FriendsUsersTable.selectAll().where { FriendsUsersTable.user eq user1.toUUID() }
			.map { it[FriendsUsersTable.friend] }
		val friendId = FriendsUsersTable.selectAll().where {
			(FriendsUsersTable.friend inList possibleFriends) and (FriendsUsersTable.user eq user2.toUUID())
		}.map { it[FriendsUsersTable.friend] }.singleOrNull() ?: return@dbQuery false
		val count1 = FriendsUsersTable.deleteWhere { friend eq friendId }
		val count2 = FriendsTable.deleteWhere { FriendsTable.id eq friendId }
		count1 == 2 && count2 == 1
	}
	
	override suspend fun addNewFriends(user1 : String, user2 : String) : Friends = dbQuery {
		val friend = FriendsTable.insert {}.resultedValues?.map { it[FriendsTable.id] }?.singleOrNull()
			?: throw Exception("Failed to create friends")
		FriendsUsersTable.insertIgnore {
			it[FriendsUsersTable.friend] = friend.value
			it[user] = user1.toUUID()
		}
		FriendsUsersTable.insertIgnore {
			it[FriendsUsersTable.friend] = friend.value
			it[user] = user2.toUUID()
		}
		Friends(friend.value)
	}
}

interface FriendRequestDao {
	suspend fun createFriendRequest(from : String, to : String) : FriendRequest?
	suspend fun acceptFriendRequest(from : String, to : String) : Boolean
	suspend fun deleteFriendRequest(from : String, to : String) : Boolean
	suspend fun friendRequestsFor(userId : String) : List<FriendRequest>
}

class FriendRequestDaoImpl : FriendRequestDao {
	override suspend fun createFriendRequest(from : String, to : String) : FriendRequest? = dbQuery {
		if (friendRequestsFor(to).any { it.fromId == from }) return@dbQuery null
		FriendRequestTable.insertIgnore {
			it[fromId] = from.toUUID()
			it[toId] = to.toUUID()
		}.resultedValues?.map(::friendRequestFromRow)?.singleOrNull()
	}
	
	override suspend fun acceptFriendRequest(from : String, to : String) : Boolean = dbQuery {
		if (FriendRequestTable.deleteWhere {
				(fromId eq from.toUUID()) and (toId eq to.toUUID())
			} > 0) {
			FriendsDaoImpl().addNewFriends(from, to)
			return@dbQuery true
		} else {
			return@dbQuery false
		}
	}
	
	override suspend fun deleteFriendRequest(from : String, to : String) : Boolean = dbQuery {
		FriendRequestTable.deleteWhere {
			(fromId eq from.toUUID()) and (toId eq to.toUUID())
		} > 0
	}
	
	override suspend fun friendRequestsFor(userId : String) : List<FriendRequest> = dbQuery {
		FriendRequestTable.selectAll().where {
			FriendRequestTable.toId eq userId.toUUID()
		}.map(::friendRequestFromRow)
	}
}

interface BlockedUsersDao {
	suspend fun blockUser(fromId : String, toId : String) : Boolean
	suspend fun unblockUser(fromId : String, toId : String) : Boolean
	suspend fun getBlockedUsers(fromId : String) : List<User>
}

class BlockedUsersDaoImpl : BlockedUsersDao {
	override suspend fun blockUser(fromId : String, toId : String) = dbQuery {
		// Delete friend requests bilaterally & delete friendship (if any exist)
		FriendRequestDaoImpl().deleteFriendRequest(fromId, toId)
		FriendRequestDaoImpl().deleteFriendRequest(toId, fromId)
		FriendsDaoImpl().deleteFriendship(fromId, toId)
		
		(BlockedUsersTable.insertIgnore {
			it[blockFrom] = fromId.toUUID()
			it[blockTo] = toId.toUUID()
		}.resultedValues?.size ?: 0) > 0
	}
	
	override suspend fun unblockUser(fromId : String, toId : String) = dbQuery {
		BlockedUsersTable.deleteWhere { (blockFrom eq fromId.toUUID()) and (blockTo eq toId.toUUID()) } > 0
	}
	
	override suspend fun getBlockedUsers(fromId : String) = dbQuery {
		val ids = BlockedUsersTable.selectAll().where {
			BlockedUsersTable.blockFrom eq fromId.toUUID()
		}.map { it[BlockedUsersTable.blockTo].value }
		UsersTable.selectAll().where { UsersTable.id inList ids }.map(::userFromRow)
	}
}
