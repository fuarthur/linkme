package com.ams.linkme.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.ams.linkme.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainViewModel : ViewModel() {
    private lateinit var firestore: FirebaseFirestore

    fun link(interest: String, callback: (List<User>) -> Unit) {
        firestore = Firebase.firestore
        val userList = mutableListOf<User>()
        firestore.collection("Users")
            .whereArrayContains("interest", interest)
            .limit(3)
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Handle query results
                for (document in querySnapshot) {
                    val user = document.toObject(User::class.java)
                    userList.add(user)
                }
                callback(userList)
            }
    }
}
