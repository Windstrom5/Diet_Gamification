package com.example.diet_gamification.firebase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.diet_gamification.room.AccountItemEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AccountFirebaseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val accountCollection = db.collection("accounts")

    private var listenerRegistration: ListenerRegistration? = null

    fun listenToAccountChanges(
        email: String,
        onChanged: (AccountItemEntity) -> Unit
    ) {
        listenerRegistration = accountCollection.document(email)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.toObject(AccountItemEntity::class.java)?.let {
                    onChanged(it)
                }
            }
    }

    fun fetchAccountByEmail(
        email: String,
        onSuccess: (AccountItemEntity?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        accountCollection.document(email).get()
            .addOnSuccessListener { document ->
                val account = document.toObject(AccountItemEntity::class.java)
                onSuccess(account)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
    fun syncToFirebase(account: AccountItemEntity) {
        accountCollection.document(account.email).set(account)
    }

    fun removeListener() {
        listenerRegistration?.remove()
    }
}
