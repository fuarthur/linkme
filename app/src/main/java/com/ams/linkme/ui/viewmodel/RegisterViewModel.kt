package com.ams.linkme.ui.viewmodel

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class RegisterViewModel : ViewModel() {

    private val _registerResultLiveData = MutableLiveData<RegisterResult>()
    val registerResultLiveData: LiveData<RegisterResult> = _registerResultLiveData

    fun register(username: String, password: String, repeatPassword: String) {
        // 验证用户名和密码
        if (username.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
            _registerResultLiveData.value = RegisterResult.Error("请填写所有字段")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            _registerResultLiveData.value = RegisterResult.Error("请输入有效的电子邮箱地址")
            return
        }

        if (password.length < 6) {
            _registerResultLiveData.value = RegisterResult.Error("密码长度必须至少为6个字符")
            return
        }

        if (password != repeatPassword) {
            _registerResultLiveData.value = RegisterResult.Error("两次输入的密码不一致")
            return
        }

        // 使用 Firebase 进行用户注册
        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.createUserWithEmailAndPassword(username, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = firebaseAuth.currentUser
                    _registerResultLiveData.value = RegisterResult.Success(user)
                } else {
                    _registerResultLiveData.value =
                        RegisterResult.Error("注册失败：" + task.exception?.message)
                }
            }
    }

    sealed class RegisterResult {
        data class Success(val user: FirebaseUser?) : RegisterResult()
        data class Error(val errorMessage: String) : RegisterResult()
    }
}
