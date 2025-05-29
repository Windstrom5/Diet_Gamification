package com.example.diet_gamification.firebase

import com.example.diet_gamification.room.WorkoutItemEntity
import com.google.firebase.firestore.FirebaseFirestore

class WorkoutItemFirebaseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val workoutCollection = db.collection("workout_items")

    fun insert(workout: WorkoutItemEntity) {
        val docId = workout.name + workout.date
        workoutCollection.document(docId).set(workout)
    }

    fun getAllNames(onComplete: (List<String>) -> Unit) {
        workoutCollection.get().addOnSuccessListener { snapshot ->
            val names = snapshot.documents.mapNotNull { it.getString("name") }.distinct()
            onComplete(names)
        }
    }

    fun getWorkoutByName(name: String, onComplete: (WorkoutItemEntity?) -> Unit) {
        workoutCollection.whereEqualTo("name", name).limit(1).get()
            .addOnSuccessListener { snapshot ->
                onComplete(snapshot.documents.firstOrNull()?.toObject(WorkoutItemEntity::class.java))
            }
    }

    fun getTotalCaloriesForWeek(
        startDate: String,
        endDate: String,
        onComplete: (Int?) -> Unit
    ) {
        workoutCollection.whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .get()
            .addOnSuccessListener { snapshot ->
                val total = snapshot.toObjects(WorkoutItemEntity::class.java)
                    .sumOf { it.calories ?: 0 }
                onComplete(total)
            }
    }
}
