package com.ams.linkme.ui.viewmodel

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterViewModel : ViewModel() {

    private val _registerResultLiveData = MutableLiveData<RegisterResult>()
    val registerResultLiveData: LiveData<RegisterResult> = _registerResultLiveData
    private lateinit var firestore: FirebaseFirestore


    fun register(email: String, password: String) {
        firestore = Firebase.firestore
        // 验证用户名和密码
        if (email.isEmpty() || password.isEmpty()) {
            _registerResultLiveData.value = RegisterResult.Error("Please fill in all fields")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _registerResultLiveData.value = RegisterResult.Error("Please enter a valid email address")
            return
        }

        if (password.length < 6) {
            _registerResultLiveData.value = RegisterResult.Error("The password must be at least 6 characters long")
            return
        }


        // 使用 Firebase 进行用户注册
        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = firebaseAuth.currentUser
                    val uid = user!!.uid
                    _registerResultLiveData.value = RegisterResult.Success(user, email, password)
                    val postMap = hashMapOf<String, Any>()
                    postMap["email"] = email

                    firestore.collection("Users").document(uid).set(postMap).addOnSuccessListener {
                        RegisterResult.Success(user, email, password)
                    }
                } else {
                    _registerResultLiveData.value =
                        RegisterResult.Error("Registration failed：" + task.exception?.message)
                }
            }
    }

    sealed class RegisterResult {
        data class Success(val user: FirebaseUser?, val email: String, val password: String) : RegisterResult()
        data class Error(val errorMessage: String) : RegisterResult()
    }
}






