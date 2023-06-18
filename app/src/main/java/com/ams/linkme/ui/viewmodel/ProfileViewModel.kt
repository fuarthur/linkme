package com.ams.linkme.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class ProfileViewModel : ViewModel() {
    private lateinit var firestore: FirebaseFirestore


    fun submit(uid: String, gender: String, phone: String, interests: ArrayList<String>) {
        firestore = Firebase.firestore
        val postMap = hashMapOf<String, Any>()
        postMap["gender"] = gender
        postMap["phone"] = phone
        postMap["interests"] = interests
        firestore.collection("Users").document(uid).set(postMap).addOnSuccessListener {

        }
    }
}




