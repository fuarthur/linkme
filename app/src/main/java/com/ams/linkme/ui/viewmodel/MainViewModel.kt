package com.ams.linkme.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.ams.linkme.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class MainViewModel : ViewModel() {
    private lateinit var firestore: FirebaseFirestore

    interface LinkCallback {
        fun onLinkResult(userList: List<User>)
    }

    fun link(interest: String, callback: LinkCallback) {
        firestore = Firebase.firestore
        val userList = mutableListOf<User>()

        // 获取登录用户的UID
        val auth = FirebaseAuth.getInstance()
        val currentUserUid = auth.currentUser?.uid

        firestore.collection("Users")
            .whereArrayContains("interests", interest)
            .whereNotEqualTo(FieldPath.documentId(), currentUserUid)
            .limit(100)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    val username = document.getString("username")
                    val email = document.getString("email")
                    val gender = document.getString("gender")
                    val phone = document.getString("phone")
                    val interests = document.get("interests") as? ArrayList<String>
                    val user = User(username, email, gender, phone, interests)
                    userList.add(user)
                }
                callback.onLinkResult(userList)
            }
    }
}

