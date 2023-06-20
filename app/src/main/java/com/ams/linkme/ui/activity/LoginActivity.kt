package com.ams.linkme.ui.activity

import com.ams.linkme.ui.viewmodel.LoginViewModel
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.ams.linkme.R

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button

    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 初始化视图
        emailEditText = findViewById(R.id.edit_text_username)
        passwordEditText = findViewById(R.id.edit_text_password)
        loginButton = findViewById(R.id.button_login)
        registerButton = findViewById(R.id.text_view_register)

        // 初始化 ViewModel
        loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        // 监听登录结果
        loginViewModel.loginResultLiveData.observe(this) { result ->
            when (result) {
                is LoginViewModel.LoginResult.Success -> {
                    navigateToMainActivity()
                }

                is LoginViewModel.LoginResult.Error -> {
                    Toast.makeText(this, result.errorMessage, Toast.LENGTH_SHORT).show()
                }

                else -> {
                    Toast.makeText(this, "Unknown error", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 点击登录按钮触发登录逻辑
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            if (email != "" && password != ""){
                loginViewModel.login(email, password)
            } else {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            }
        }

        registerButton.setOnClickListener {
            navigateToRegisterActivity()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToRegisterActivity() {
        val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
        startActivity(intent)
        finish()
    }
}
