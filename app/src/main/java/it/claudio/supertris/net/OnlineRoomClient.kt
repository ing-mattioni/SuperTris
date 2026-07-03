package it.claudio.supertris.net

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import it.claudio.supertris.core.GameMode
import it.claudio.supertris.core.GameState
import it.claudio.supertris.core.Move
import it.claudio.supertris.core.SuperTrisRules
import java.util.Date
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

const val ONLINE_PROTOCOL_VERSION = 1

data class RoomMove(val index: Int, val micro: Int, val cell: Int)

data class OnlineRoom(
    val code: String,
    val status: String, // waiting | playing | finished
    val hostUid: String,
    val guestUid: String?,
    val hostName: String,
    val guestName: String?,
    val hostMark: Int,
    val firstTurn: Int,
    val round: Int,
    val hostRematch: Boolean,
    val guestRematch: Boolean,
    val moves: List<RoomMove>,
    val hostSeenMs: Long?,
    val guestSeenMs: Long?,
    val protocolVersion: Int,
)

/** Ricostruisce lo stato locale applicando le mosse in ordine. Null se una mossa e' illegale (desync). */
fun OnlineRoom.buildLocalState(localIsHost: Boolean): GameState? {
    val localMark = if (localIsHost) hostMark else SuperTrisRules.other(hostMark)
    var st = SuperTrisRules.newRemoteGame(
        localMark = localMark,
        firstTurn = firstTurn,
        gameMode = GameMode.ONLINE,
    )
    for ((i, m) in moves.withIndex()) {
        if (m.index != i) return null
        val move = Move(micro = m.micro, cell = m.cell)
        if (!SuperTrisRules.isLegalMove(st, move)) return null
        st = SuperTrisRules.applyMove(st, move)
    }
    return st
}

sealed interface JoinResult {
    data class Ok(val room: OnlineRoom) : JoinResult
    data object NotFound : JoinResult
    data object Full : JoinResult
    data object VersionMismatch : JoinResult
}

/**
 * Client Firestore per le stanze online. Nessuno stato di gioco qui dentro:
 * il documento della stanza e' la fonte di verita', osservata via [observeRoom].
 */
class OnlineRoomClient {

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    val uid: String? get() = auth.currentUser?.uid

    suspend fun ensureSignedIn(): String {
        auth.currentUser?.let { return it.uid }
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: error("Autenticazione anonima fallita")
    }

    suspend fun createRoom(nickname: String, random: Random = Random.Default): OnlineRoom {
        val myUid = ensureSignedIn()

        repeat(CREATE_ATTEMPTS) {
            val code = generateRoomCode(random)
            val hostMark = if (random.nextBoolean()) SuperTrisRules.X else SuperTrisRules.O
            val firstTurn = if (random.nextBoolean()) SuperTrisRules.X else SuperTrisRules.O
            val ref = db.collection(ROOMS).document(code)

            val created = db.runTransaction { tx ->
                if (tx.get(ref).exists()) {
                    false
                } else {
                    tx.set(
                        ref,
                        mapOf(
                            "status" to "waiting",
                            "hostUid" to myUid,
                            "guestUid" to null,
                            "hostName" to nickname,
                            "guestName" to null,
                            "hostMark" to hostMark,
                            "firstTurn" to firstTurn,
                            "round" to 0,
                            "hostRematch" to false,
                            "guestRematch" to false,
                            "moves" to emptyList<Map<String, Any>>(),
                            "hostSeen" to FieldValue.serverTimestamp(),
                            "guestSeen" to null,
                            "protocolVersion" to ONLINE_PROTOCOL_VERSION,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "expireAt" to newExpireAt(),
                        ),
                    )
                    true
                }
            }.await()

            if (created) {
                return OnlineRoom(
                    code = code,
                    status = "waiting",
                    hostUid = myUid,
                    guestUid = null,
                    hostName = nickname,
                    guestName = null,
                    hostMark = hostMark,
                    firstTurn = firstTurn,
                    round = 0,
                    hostRematch = false,
                    guestRematch = false,
                    moves = emptyList(),
                    hostSeenMs = System.currentTimeMillis(),
                    guestSeenMs = null,
                    protocolVersion = ONLINE_PROTOCOL_VERSION,
                )
            }
        }
        error("Impossibile generare un codice stanza libero")
    }

    suspend fun joinRoom(code: String, nickname: String): JoinResult {
        val myUid = ensureSignedIn()
        val ref = db.collection(ROOMS).document(code.uppercase().trim())

        return db.runTransaction { tx ->
            val snap = tx.get(ref)
            if (!snap.exists()) return@runTransaction JoinResult.NotFound

            val room = parseRoom(snap) ?: return@runTransaction JoinResult.NotFound
            if (room.protocolVersion != ONLINE_PROTOCOL_VERSION) return@runTransaction JoinResult.VersionMismatch

            when {
                room.hostUid == myUid || room.guestUid == myUid -> JoinResult.Ok(room) // rientro
                room.guestUid != null -> JoinResult.Full
                else -> {
                    tx.update(
                        ref,
                        mapOf(
                            "guestUid" to myUid,
                            "guestName" to nickname,
                            "status" to "playing",
                            "guestSeen" to FieldValue.serverTimestamp(),
                            "expireAt" to newExpireAt(),
                        ),
                    )
                    JoinResult.Ok(
                        room.copy(
                            guestUid = myUid,
                            guestName = nickname,
                            status = "playing",
                        ),
                    )
                }
            }
        }.await()
    }

    /** Rientro in una stanza salvata: null se non esiste piu' o non sei un partecipante. */
    suspend fun fetchRoomIfParticipant(code: String): OnlineRoom? {
        val myUid = ensureSignedIn()
        val snap = db.collection(ROOMS).document(code).get().await()
        val room = parseRoom(snap) ?: return null
        return room.takeIf { it.hostUid == myUid || it.guestUid == myUid }
    }

    fun observeRoom(code: String): Flow<OnlineRoom?> = callbackFlow {
        val registration = db.collection(ROOMS).document(code)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(null)
                } else {
                    trySend(snap?.let { parseRoom(it) })
                }
            }
        awaitClose { registration.remove() }
    }

    /** Aggiunge la mossa se l'indice atteso corrisponde (transazione anti-race). */
    suspend fun sendMove(code: String, expectedIndex: Int, micro: Int, cell: Int, isHost: Boolean) {
        val ref = db.collection(ROOMS).document(code)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val room = parseRoom(snap) ?: throw FirebaseFirestoreException(
                "Stanza inesistente",
                FirebaseFirestoreException.Code.NOT_FOUND,
            )
            if (room.moves.size != expectedIndex) {
                throw FirebaseFirestoreException(
                    "Indice mossa non allineato",
                    FirebaseFirestoreException.Code.ABORTED,
                )
            }
            val newMoves = room.moves.map { it.toMap() } +
                mapOf("i" to expectedIndex, "micro" to micro, "cell" to cell)
            tx.update(
                ref,
                mapOf(
                    "moves" to newMoves,
                    seenField(isHost) to FieldValue.serverTimestamp(),
                    "expireAt" to newExpireAt(),
                ),
            )
        }.await()
    }

    suspend fun requestRematch(code: String, isHost: Boolean) {
        val field = if (isHost) "hostRematch" else "guestRematch"
        db.collection(ROOMS).document(code)
            .update(
                mapOf(
                    field to true,
                    seenField(isHost) to FieldValue.serverTimestamp(),
                    "expireAt" to newExpireAt(),
                ),
            ).await()
    }

    /** Solo l'host: fa ripartire la stanza quando entrambi hanno chiesto la rivincita. */
    suspend fun restartIfBothAgreed(code: String, random: Random = Random.Default) {
        val ref = db.collection(ROOMS).document(code)
        db.runTransaction { tx ->
            val room = parseRoom(tx.get(ref)) ?: return@runTransaction
            if (!room.hostRematch || !room.guestRematch) return@runTransaction

            tx.update(
                ref,
                mapOf(
                    "moves" to emptyList<Map<String, Any>>(),
                    "hostMark" to if (random.nextBoolean()) SuperTrisRules.X else SuperTrisRules.O,
                    "firstTurn" to if (random.nextBoolean()) SuperTrisRules.X else SuperTrisRules.O,
                    "round" to room.round + 1,
                    "hostRematch" to false,
                    "guestRematch" to false,
                    "status" to "playing",
                    "expireAt" to newExpireAt(),
                ),
            )
        }.await()
    }

    suspend fun heartbeat(code: String, isHost: Boolean) {
        runCatching {
            db.collection(ROOMS).document(code)
                .update(
                    mapOf(
                        seenField(isHost) to FieldValue.serverTimestamp(),
                        "expireAt" to newExpireAt(),
                    ),
                ).await()
        }
    }

    /** Uscita esplicita: la stanza resta comunque soggetta al TTL. */
    suspend fun leaveRoom(code: String, isHost: Boolean, stillWaiting: Boolean) {
        runCatching {
            val ref = db.collection(ROOMS).document(code)
            if (isHost && stillWaiting) {
                ref.delete().await()
            } else {
                ref.update("status", "finished").await()
            }
        }
    }

    private fun RoomMove.toMap(): Map<String, Any> =
        mapOf("i" to index, "micro" to micro, "cell" to cell)

    private fun seenField(isHost: Boolean) = if (isHost) "hostSeen" else "guestSeen"

    private fun newExpireAt(): Timestamp =
        Timestamp(Date(System.currentTimeMillis() + ROOM_TTL_MS))

    private fun parseRoom(snap: DocumentSnapshot): OnlineRoom? {
        if (!snap.exists()) return null
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            val rawMoves = snap.get("moves") as? List<Map<String, Any>> ?: emptyList()
            OnlineRoom(
                code = snap.id,
                status = snap.getString("status") ?: return null,
                hostUid = snap.getString("hostUid") ?: return null,
                guestUid = snap.getString("guestUid"),
                hostName = snap.getString("hostName") ?: "?",
                guestName = snap.getString("guestName"),
                hostMark = (snap.getLong("hostMark") ?: return null).toInt(),
                firstTurn = (snap.getLong("firstTurn") ?: return null).toInt(),
                round = (snap.getLong("round") ?: 0L).toInt(),
                hostRematch = snap.getBoolean("hostRematch") ?: false,
                guestRematch = snap.getBoolean("guestRematch") ?: false,
                moves = rawMoves.map {
                    RoomMove(
                        index = (it["i"] as Number).toInt(),
                        micro = (it["micro"] as Number).toInt(),
                        cell = (it["cell"] as Number).toInt(),
                    )
                },
                hostSeenMs = snap.getTimestamp("hostSeen")?.toDate()?.time,
                guestSeenMs = snap.getTimestamp("guestSeen")?.toDate()?.time,
                protocolVersion = (snap.getLong("protocolVersion") ?: 0L).toInt(),
            )
        }.getOrNull()
    }

    companion object {
        private const val ROOMS = "rooms"
        private const val CREATE_ATTEMPTS = 5
        private const val ROOM_TTL_MS = 24L * 60 * 60 * 1000

        // Alfabeto senza caratteri ambigui (niente 0/O, 1/I/L).
        private const val CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
        const val CODE_LENGTH = 5

        fun generateRoomCode(random: Random = Random.Default): String =
            buildString(CODE_LENGTH) {
                repeat(CODE_LENGTH) { append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]) }
            }

        fun isValidCodeFormat(code: String): Boolean {
            val c = code.uppercase().trim()
            return c.length == CODE_LENGTH && c.all { CODE_ALPHABET.contains(it) }
        }

        /** True se questa build include la configurazione Firebase (google-services.json). */
        fun isAvailable(context: Context): Boolean =
            runCatching { FirebaseApp.initializeApp(context) != null }.getOrDefault(false)
    }
}
