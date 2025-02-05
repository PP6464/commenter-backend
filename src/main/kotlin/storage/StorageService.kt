package app.web.commenter_api.storage

import com.google.auth.oauth2.*
import com.google.cloud.storage.*
import com.google.firebase.*
import com.google.firebase.cloud.*
import io.github.cdimascio.dotenv.*
import io.ktor.http.*
import java.io.*

fun storageURLFor(path : String) : String {
	val dotenv = dotenv {}
	val pathEncoded = path.encodeURLPath(encodeSlash = true)
	return "https://firebasestorage.googleapis.com/v0/b/${dotenv["STORAGE_BUCKET_NAME"]}/o/$pathEncoded?alt=media"
}

class StorageService {
	private val app : FirebaseApp
	private val storage : Storage
	private val bucketName : String
	
	init {
		val dotenv = dotenv {}
		bucketName = dotenv["STORAGE_BUCKET_NAME"]
		val credentialsStream = FileInputStream(dotenv["GOOGLE_CREDENTIALS"])
		val options = FirebaseOptions
			.builder()
			.setCredentials(GoogleCredentials.fromStream(credentialsStream))
			.setStorageBucket(dotenv["STORAGE_BUCKET_NAME"])
			.build()
		
		app = FirebaseApp.initializeApp(options)
		
		storage = StorageClient
			.getInstance(app)
			.bucket(bucketName)
			.storage
	}
	
	fun delete(path : String) : Boolean {
		return storage[path].delete()
	}
	
	fun upload(path : String, file : ByteArray, mimeType : String) : String {
		val blobInfo = BlobInfo
			.newBuilder(bucketName, path)
			.setContentType(mimeType)
			.build()
		storage.create(blobInfo, file)
		return storageURLFor(path)
	}
	
	fun deleteDir(path : String) {
		storage.list(bucketName, Storage.BlobListOption.prefix(path)).iterateAll().map { it.delete() }
	}
}