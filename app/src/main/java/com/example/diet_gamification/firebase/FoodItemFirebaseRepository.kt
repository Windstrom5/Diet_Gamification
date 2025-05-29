package com.example.diet_gamification.firebase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.diet_gamification.room.FoodItemEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration

class FoodItemFirebaseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val foodItemsCollection = db.collection("food_items")

    fun insert(food: FoodItemEntity) {
        val docId = food.name + food.date + (food.id_account ?: "no_account")
        foodItemsCollection.document(docId).set(food)
    }

    fun getFoodsByCategory(category: String): LiveData<List<FoodItemEntity>> {
        val liveData = MutableLiveData<List<FoodItemEntity>>()
        foodItemsCollection.whereEqualTo("category", category)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.toObjects(FoodItemEntity::class.java) ?: emptyList()
                liveData.value = list
            }
        return liveData
    }

    suspend fun getAllNames(onComplete: (List<String>) -> Unit) {
        foodItemsCollection.get().addOnSuccessListener { snapshot ->
            val names = snapshot.documents.mapNotNull { it.getString("name") }.distinct()
            onComplete(names)
        }
    }

    suspend fun getFoodByName(name: String, onComplete: (FoodItemEntity?) -> Unit) {
        foodItemsCollection.whereEqualTo("name", name).limit(1).get()
            .addOnSuccessListener { snapshot ->
                onComplete(snapshot.documents.firstOrNull()?.toObject(FoodItemEntity::class.java))
            }
    }

    fun getFoodItemsByCategoryAndDate(
        category: String,
        date: String,
        id: Int?,
        onComplete: (List<FoodItemEntity>) -> Unit
    ) {
        var query: Query = foodItemsCollection
            .whereEqualTo("category", category)
            .whereEqualTo("date", date)

        if (id != null) {
            query = query.whereEqualTo("id_account", id)
        } else {
            query = query.whereEqualTo("id_account", null)
        }

        query.get().addOnSuccessListener { snapshot ->
            onComplete(snapshot.toObjects(FoodItemEntity::class.java))
        }
    }

    fun getTotalCaloriesForDateAndAccount(
        date: String,
        accountId: Int,
        onComplete: (Int?) -> Unit
    ) {
        foodItemsCollection
            .whereEqualTo("date", date)
            .whereEqualTo("id_account", accountId)
            .get()
            .addOnSuccessListener { snapshot ->
                val total = snapshot.toObjects(FoodItemEntity::class.java)
                    .sumOf { it.calories ?: 0 }
                onComplete(total)
            }
    }
}
