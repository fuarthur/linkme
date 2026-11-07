package com.ams.linkme.data

import com.ams.linkme.domain.InterestCatalog
import com.ams.linkme.domain.Interests
import com.ams.linkme.domain.LinkMeLogic
import com.ams.linkme.model.Message
import com.ams.linkme.model.StreamUpdate
import com.ams.linkme.model.User
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source

class LinkMeRepository private constructor() {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val publicProfiles = mutableMapOf<String, User>()
    val uid: String?
        get() = auth.currentUser?.uid

    fun signIn(email: String, password: String, callback: (Result<Unit>) -> Unit) {
        auth
            .signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun register(email: String, password: String, callback: (Result<Unit>) -> Unit) {
        auth
            .createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    callback(Result.failure(IllegalStateException("Please sign in again.")))
                    return@addOnSuccessListener
                }
                db.collection("Users")
                    .document(user.uid)
                    .set(
                        mapOf(
                            "uid" to user.uid,
                            "email" to email.trim(),
                            "username" to "",
                            "bio" to "",
                            "interests" to emptyList<String>(),
                            "discoverable" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    )
                    .addOnSuccessListener { callback(Result.success(Unit)) }
                    .addOnFailureListener {
                        auth.signOut()
                        callback(
                            Result.failure(
                                IllegalStateException(
                                    "Your account was created, but setup could not finish. Sign in to finish your profile.",
                                    it,
                                )
                            )
                        )
                    }
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun resetPassword(email: String, callback: (Result<Unit>) -> Unit) {
        auth
            .sendPasswordResetEmail(email.trim())
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun loadProfile(callback: (Result<User>) -> Unit) {
        val id =
            uid ?: return callback(Result.failure(IllegalStateException("Please sign in again.")))
        db.collection("Users")
            .document(id)
            .get(Source.SERVER)
            .addOnSuccessListener { callback(Result.success(profile(it, id))) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun saveProfile(user: User, callback: (Result<Unit>) -> Unit) {
        val id =
            uid ?: return callback(Result.failure(IllegalStateException("Please sign in again.")))
        val validation = LinkMeLogic.validateProfile(user.username, user.bio, user.interests)
        if (validation != null)
            return callback(Result.failure(IllegalArgumentException(validation)))
        val fields =
            mapOf(
                "uid" to id,
                "username" to user.username.trim(),
                "bio" to user.bio.trim(),
                "interests" to InterestCatalog.normalize(user.interests),
                "interestKeys" to InterestCatalog.indexKeys(user.interests),
                "discoverable" to user.discoverable,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        db.batch()
            .apply {
                set(db.collection("Users").document(id), fields, SetOptions.merge())
                set(db.collection("PublicProfiles").document(id), fields)
            }
            .commit()
            .addOnSuccessListener {
                publicProfiles[id] = user.copy(uid = id)
                callback(Result.success(Unit))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun discover(interests: List<String>, callback: (Result<List<User>>) -> Unit) {
        val keys = InterestCatalog.indexKeys(interests)
        if (keys.isEmpty()) return callback(Result.success(emptyList()))
        val tasks = mutableListOf<Task<QuerySnapshot>>()
        val visible = db.collection("PublicProfiles").whereEqualTo("discoverable", true)
        tasks.add(visible.whereArrayContainsAny("interestKeys", keys).limit(60).get(Source.SERVER))
        val legacy = InterestCatalog.legacyQueryValues(interests)
        if (legacy.isNotEmpty())
            tasks.add(
                visible.whereArrayContainsAny("interests", legacy).limit(60).get(Source.SERVER)
            )
        Tasks.whenAllSuccess<QuerySnapshot>(tasks)
            .addOnSuccessListener { snapshots ->
                val people =
                    snapshots
                        .flatMap { it.documents }
                        .distinctBy { it.id }
                        .map { profile(it, it.id) }
                people.forEach { publicProfiles[it.uid] = it }
                callback(Result.success(people))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun publicProfile(id: String, refresh: Boolean = false, callback: (User?) -> Unit) {
        if (!refresh)
            publicProfiles[id]?.let {
                callback(it)
                return
            }
        db.collection("PublicProfiles")
            .document(id)
            .get()
            .addOnSuccessListener {
                val user = if (it.exists()) profile(it, id) else null
                if (user != null) publicProfiles[id] = user
                callback(user)
            }
            .addOnFailureListener { callback(null) }
    }

    fun observeMessages(
        peer: String,
        callback: (Result<StreamUpdate<Message>>) -> Unit,
    ): ListenerRegistration? {
        val id = uid ?: return null
        if (peer.isBlank() || peer == id) {
            callback(
                Result.failure(IllegalArgumentException("Choose another person to chat with."))
            )
            return null
        }
        val query =
            db.collection("Messages")
                .where(
                    Filter.or(
                        Filter.and(
                            Filter.equalTo("senderId", id),
                            Filter.equalTo("receiveId", peer),
                        ),
                        Filter.and(
                            Filter.equalTo("senderId", peer),
                            Filter.equalTo("receiveId", id),
                        ),
                    )
                )
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .orderBy(
                    com.google.firebase.firestore.FieldPath.documentId(),
                    Query.Direction.DESCENDING,
                )
                .limit(100)
        return query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                callback(Result.failure(error))
                return@addSnapshotListener
            }
            if (snapshot != null)
                callback(
                    Result.success(
                        StreamUpdate(
                            snapshot.documents.map(::message).reversed(),
                            snapshot.metadata.isFromCache,
                        )
                    )
                )
        }
    }

    fun olderMessages(peer: String, before: Message, callback: (Result<List<Message>>) -> Unit) {
        val id =
            uid ?: return callback(Result.failure(IllegalStateException("Please sign in again.")))
        if (before.timestamp == null && before.timestampSeconds == null)
            return callback(Result.success(emptyList()))
        val cursor = LinkMeLogic.paginationCursor(before)
        db.collection("Messages")
            .where(
                Filter.or(
                    Filter.and(Filter.equalTo("senderId", id), Filter.equalTo("receiveId", peer)),
                    Filter.and(Filter.equalTo("senderId", peer), Filter.equalTo("receiveId", id)),
                )
            )
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .orderBy(
                com.google.firebase.firestore.FieldPath.documentId(),
                Query.Direction.DESCENDING,
            )
            .startAfter(com.google.firebase.Timestamp(cursor.first, cursor.second), before.id)
            .limit(100)
            .get(Source.SERVER)
            .addOnSuccessListener {
                callback(Result.success(it.documents.map(::message).reversed()))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    private fun inboxQuery(id: String): Query =
        db.collection("Messages")
            .where(Filter.or(Filter.equalTo("senderId", id), Filter.equalTo("receiveId", id)))
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .orderBy(
                com.google.firebase.firestore.FieldPath.documentId(),
                Query.Direction.DESCENDING,
            )

    fun observeInbox(callback: (Result<StreamUpdate<Message>>) -> Unit): ListenerRegistration? {
        val id = uid ?: return null
        return inboxQuery(id).limit(100).addSnapshotListener(MetadataChanges.INCLUDE) {
            snapshot,
            error ->
            if (error != null) {
                callback(Result.failure(error))
                return@addSnapshotListener
            }
            if (snapshot != null)
                callback(
                    Result.success(
                        StreamUpdate(
                            snapshot.documents.map(::message),
                            snapshot.metadata.isFromCache,
                        )
                    )
                )
        }
    }

    fun olderInbox(before: Message, callback: (Result<List<Message>>) -> Unit) {
        val id =
            uid ?: return callback(Result.failure(IllegalStateException("Please sign in again.")))
        if (before.timestamp == null && before.timestampSeconds == null)
            return callback(Result.success(emptyList()))
        val cursor = LinkMeLogic.paginationCursor(before)
        inboxQuery(id)
            .startAfter(com.google.firebase.Timestamp(cursor.first, cursor.second), before.id)
            .limit(100)
            .get(Source.SERVER)
            .addOnSuccessListener { callback(Result.success(it.documents.map(::message))) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun observeChats(
        callback: (Result<List<com.ams.linkme.model.Conversation>>) -> Unit
    ): ListenerRegistration? {
        val id = uid ?: return null
        return db.collection("Chats")
            .whereArrayContains("members", id)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    callback(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null)
                    callback(
                        Result.success(
                            snapshot.documents.mapNotNull { doc ->
                                val members =
                                    (doc.get("members") as? List<*>)
                                        ?.filterIsInstance<String>()
                                        .orEmpty()
                                val peer =
                                    members.firstOrNull { it != id } ?: return@mapNotNull null
                                val sender = doc.getString("lastSenderId").orEmpty()
                                val stamp =
                                    doc.getTimestamp(
                                        "updatedAt",
                                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                                    )
                                com.ams.linkme.model.Conversation(
                                    peer,
                                    Message(
                                        doc.id,
                                        sender,
                                        if (sender == id) peer else id,
                                        doc.getString("lastMessage").orEmpty(),
                                        stamp?.toDate()?.time,
                                        doc.metadata.hasPendingWrites(),
                                        stamp?.seconds,
                                        stamp?.nanoseconds ?: 0,
                                    ),
                                )
                            }
                        )
                    )
            }
    }

    fun newMessageId(): String = db.collection("Messages").document().id

    fun send(peer: String, content: String, messageId: String, callback: (Result<Unit>) -> Unit) {
        val id =
            uid ?: return callback(Result.failure(IllegalStateException("Please sign in again.")))
        val error = LinkMeLogic.validateMessage(content)
        if (error != null) return callback(Result.failure(IllegalArgumentException(error)))
        val chatId =
            try {
                LinkMeLogic.conversationId(id, peer)
            } catch (e: IllegalArgumentException) {
                return callback(Result.failure(e))
            }
        val members = listOf(id, peer).sorted()
        val summary =
            mapOf(
                "members" to members,
                "lastMessage" to content.trim(),
                "lastMessageId" to messageId,
                "lastSenderId" to id,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        val fields =
            mapOf(
                "senderId" to id,
                "receiveId" to peer,
                "content" to content.trim(),
                "timestamp" to FieldValue.serverTimestamp(),
                "conversationId" to chatId,
                "members" to members,
            )
        db.batch()
            .apply {
                set(db.collection("Chats").document(chatId), summary)
                set(db.collection("Messages").document(messageId), fields)
            }
            .commit()
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun blocked(callback: (Result<Set<String>>) -> Unit) {
        val id =
            uid ?: return callback(Result.failure(IllegalStateException("Please sign in again.")))
        db.collection("Users")
            .document(id)
            .collection("Blocked")
            .limit(100)
            .get(Source.SERVER)
            .addOnSuccessListener {
                callback(Result.success(it.documents.map { doc -> doc.id }.toSet()))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun setBlocked(peer: String, blocked: Boolean, callback: (Result<Unit>) -> Unit) {
        val id =
            uid ?: return callback(Result.failure(IllegalStateException("Please sign in again.")))
        val ref = db.collection("Users").document(id).collection("Blocked").document(peer)
        val task =
            if (blocked) ref.set(mapOf("createdAt" to FieldValue.serverTimestamp()))
            else ref.delete()
        task
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun signOut() {
        publicProfiles.clear()
        auth.signOut()
    }

    private fun profile(doc: DocumentSnapshot, id: String) =
        User(
            uid = id,
            username = doc.getString("username").orEmpty(),
            bio = doc.getString("bio").orEmpty(),
            interests =
                Interests.normalize(
                    (doc.get("interests") as? List<*>)?.filterIsInstance<String>().orEmpty()
                ),
            discoverable = doc.getBoolean("discoverable") ?: false,
        )

    private fun message(doc: DocumentSnapshot): Message {
        val stamp = doc.getTimestamp("timestamp", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        return Message(
            id = doc.id,
            senderId = doc.getString("senderId").orEmpty(),
            receiveId = doc.getString("receiveId").orEmpty(),
            content = doc.getString("content").orEmpty(),
            timestamp = stamp?.toDate()?.time,
            pending = doc.metadata.hasPendingWrites(),
            timestampSeconds = stamp?.seconds,
            timestampNanoseconds = stamp?.nanoseconds ?: 0,
        )
    }

    companion object {
        val instance: LinkMeRepository by lazy { LinkMeRepository() }
    }
}

fun Throwable.userMessage(): String =
    when (this) {
        is FirebaseAuthException ->
            when (errorCode) {
                "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
                "ERROR_EMAIL_ALREADY_IN_USE" ->
                    "This email already has an account. Sign in instead."
                "ERROR_WEAK_PASSWORD" -> "Use a password with at least 6 characters."
                "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Wait a little and try again."
                "ERROR_USER_DISABLED" -> "This account is unavailable."
                else -> "Could not sign in. Check your email and password, then try again."
            }
        is FirebaseFirestoreException ->
            when (code) {
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    "Cannot connect right now. Check your connection and try again."
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "This action is unavailable. The profile may be private, or one of you has blocked the other."
                FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                    "This list is not ready yet. Please try again shortly."
                else -> "Could not finish. Please try again."
            }
        is IllegalArgumentException,
        is IllegalStateException -> message ?: "Please try again."
        else -> "Cannot connect right now. Check your connection and try again."
    }
