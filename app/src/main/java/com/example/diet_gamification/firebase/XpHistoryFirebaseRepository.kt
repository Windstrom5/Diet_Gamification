package com.example.diet_gamification.firebase

import com.example.diet_gamification.room.XpHistoryEntity
import com.google.firebase.firestore.FirebaseFirestore

class XpHistoryFirebaseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val xpCollection = db.collection("xp_history")

    fun insertXpHistory(xp: XpHistoryEntity) {
        val docId = "${xp.accountId}_${xp.date}_${xp.category}"
        xpCollection.document(docId).set(xp)
    }

    fun updateXpHistory(xp: XpHistoryEntity) {
        val docId = "${xp.accountId}_${xp.date}_${xp.category}"
        xpCollection.document(docId).set(xp)
    }

    fun getXpHistoryByDateAndCategory(
        date: String,
        accountId: Int,
        category: String,
        onComplete: (XpHistoryEntity?) -> Unit
    ) {
        val docId = "${accountId}_${date}_${category}"
        xpCollection.document(docId).get()
            .addOnSuccessListener { snapshot ->
                onComplete(snapshot.toObject(XpHistoryEntity::class.java))
            }
    }

    fun getXpHistoryForWeek(
        accountId: Int,
        startDate: String,
        endDate: String,
        onComplete: (List<XpHistoryEntity>) -> Unit
    ) {
        xpCollection.whereEqualTo("accountId", accountId)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .get()
            .addOnSuccessListener { snapshot ->
                onComplete(snapshot.toObjects(XpHistoryEntity::class.java))
            }
    }
}
