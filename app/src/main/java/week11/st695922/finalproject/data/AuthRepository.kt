package week11.st695922.finalproject.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import week11.st695922.finalproject.model.UserProfile
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Isolates every FirebaseAuth call behind suspend functions (Week 6.1, Slides 9-11):
 * each callback-based SDK call is wrapped in suspendCancellableCoroutine so the
 * ViewModel can call it like a normal suspend function inside viewModelScope.launch.
 * Per Week 6.1, Slide 6, Compose UI never touches FirebaseAuth directly.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Restores session state on app start ("Splash / session check", per the
     * screen design). FirebaseAuth keeps the signed-in user across process
     * restarts on its own; this just observes that state as a Flow.
     */
    fun authStateFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String): Result<String> =
        suspendCancellableCoroutine { cont ->
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        cont.resume(Result.success(auth.currentUser?.uid.orEmpty()))
                    } else {
                        cont.resume(Result.failure(task.exception ?: Exception("Sign in failed")))
                    }
                }
        }

    /**
     * Creates the Firebase Auth user, then writes the matching `users/{uid}`
     * profile document (Week 6.2, Slide 6) in the same call so every signed-up
     * user has a profile doc to attach a home station to later.
     */
    suspend fun signUp(fullName: String, email: String, password: String): Result<String> =
        suspendCancellableCoroutine { cont ->
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid.orEmpty()
                        val profile = UserProfile(uid = uid, fullName = fullName, email = email)
                        firestore.collection("users").document(uid).set(profile)
                            .addOnSuccessListener { cont.resume(Result.success(uid)) }
                            .addOnFailureListener { e ->
                                if (cont.isActive) cont.resumeWithException(e)
                            }
                    } else {
                        cont.resume(Result.failure(task.exception ?: Exception("Sign up failed")))
                    }
                }
        }

    fun signOut() {
        auth.signOut()
    }
}
