package com.ams.linkme.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class ProfileViewModel : ViewModel() {
    private lateinit var firestore: FirebaseFirestore


    fun submit(name: String, uid: String, gender: String, phone: String, interests: ArrayList<String>) {
        firestore = Firebase.firestore

        // 获取当前用户的现有数据
        firestore.collection("Users").document(uid).get().addOnSuccessListener { document ->
            if (document.exists()) {
                // 获取现有数据
                val existingData = document.data

                // 创建新的数据更新映射
                val updatedData = HashMap<String, Any>(existingData ?: hashMapOf())
                updatedData["username"] = name
                updatedData["gender"] = gender
                updatedData["phone"] = phone
                updatedData["interests"] = interests

                // 更新数据
                firestore.collection("Users").document(uid).set(updatedData).addOnSuccessListener {
                    // 更新成功
                }
            }
        }
    }

}




