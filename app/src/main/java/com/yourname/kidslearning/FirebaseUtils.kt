package com.yourname.kidslearning

import com.google.firebase.database.FirebaseDatabase

private const val DB_URL = "https://kinderjoy-372c6-default-rtdb.asia-southeast1.firebasedatabase.app"

fun saveTimeLimitToFirebase(hours: Int, minutes: Int) {
    val db = FirebaseDatabase.getInstance(DB_URL)
    val ref = db.getReference("parentalSettings/screenTimeLimit")

    val timeData = mapOf(
        "hours" to hours,
        "minutes" to minutes
    )

    ref.setValue(timeData)
}

fun fetchTimeLimitFromFirebase(onResult: (Int, Int) -> Unit) {
    val db = FirebaseDatabase.getInstance(DB_URL)
    val ref = db.getReference("parentalSettings/screenTimeLimit")

    ref.get().addOnSuccessListener { dataSnapshot ->
        val hours = dataSnapshot.child("hours").getValue(Int::class.java) ?: 0
        val minutes = dataSnapshot.child("minutes").getValue(Int::class.java) ?: 0
        onResult(hours, minutes)
    }
}

fun saveProgressToFirebase(sectionName: String, durationMinutes: Int) {
    val db = FirebaseDatabase.getInstance(DB_URL)
    val ref = db.getReference("sectionProgress/$sectionName")

    ref.get().addOnSuccessListener { snapshot ->
        val existingTime = snapshot.getValue(Int::class.java) ?: 0
        ref.setValue(existingTime + durationMinutes)
    }
}

fun resetProgressDataToDefault() {
    val defaultData = mapOf(
        "Numbers" to 0,
        "Alphabets" to 0,
        "Shapes" to 0,
        "Coloring" to 0,
        "Mythology" to 0
    )
    val ref = FirebaseDatabase.getInstance(DB_URL).getReference("sectionProgress")
    ref.setValue(defaultData)
}

fun fetchSectionProgressFromFirebase(onResult: (Map<String, Int>) -> Unit) {
    val ref = FirebaseDatabase.getInstance(DB_URL).getReference("sectionProgress")
    ref.get()
        .addOnSuccessListener { snapshot ->
            val sectionMap = mutableMapOf<String, Int>()
            snapshot.children.forEach { child ->
                val section = child.key ?: return@forEach
                val value = child.getValue(Int::class.java) ?: 0
                sectionMap[section] = value
            }
            onResult(sectionMap)
        }
        .addOnFailureListener {
            it.printStackTrace()
            onResult(emptyMap())
        }
}
