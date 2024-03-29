package com.ams.linkme.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.ams.linkme.R
import com.ams.linkme.ui.viewmodel.LoginViewModel
import com.ams.linkme.ui.viewmodel.RegisterViewModel


class RegisterActivity : AppCompatActivity() {

    private lateinit var passwordEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var backButton: Button
    private lateinit var registerViewModel: RegisterViewModel
    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // 初始化视图
        emailEditText = findViewById(R.id.edit_text_email)
        passwordEditText = findViewById(R.id.edit_text_password)
        registerButton = findViewById(R.id.button_register)
        backButton = findViewById(R.id.button_back)

        // 初始化 ViewModel
        registerViewModel = ViewModelProvider(this)[RegisterViewModel::class.java]
        loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        // 监听注册结果
        registerViewModel.registerResultLiveData.observe(this) { result ->
            when (result) {
                is RegisterViewModel.RegisterResult.Success -> {
                    Toast.makeText(this, "Registration successful, please set your personal profile", Toast.LENGTH_SHORT).show()
                    loginViewModel.login(result.email, result.password)
                    navigateToProfileActivity()
                }

                is RegisterViewModel.RegisterResult.Error -> {
                    Toast.makeText(this, result.errorMessage, Toast.LENGTH_SHORT).show()
                }

                else -> {
                    Toast.makeText(this, "Unknown error", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 点击注册按钮触发注册逻辑
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            registerViewModel.register(email, password)
        }

        backButton.setOnClickListener {
            navigateToLoginActivity()
        }
    }

    private fun navigateToProfileActivity() {
        val intent = Intent(this@RegisterActivity, ProfileActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
