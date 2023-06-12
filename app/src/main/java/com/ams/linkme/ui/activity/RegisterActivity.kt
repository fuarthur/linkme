package com.ams.linkme.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.ams.linkme.R
import com.ams.linkme.ui.viewmodel.RegisterViewModel

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var repeatPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var registerViewModel: RegisterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // 初始化视图
        usernameEditText = findViewById(R.id.edit_text_username)
        passwordEditText = findViewById(R.id.edit_text_password)
        repeatPasswordEditText = findViewById(R.id.repeat_text_password)
        registerButton = findViewById(R.id.button_register)

        // 初始化 ViewModel
        registerViewModel = ViewModelProvider(this)[RegisterViewModel::class.java]

        // 监听注册结果
        registerViewModel.registerResultLiveData.observe(this) { result ->
            when (result) {
                is RegisterViewModel.RegisterResult.Success -> {
                    Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                }

                is RegisterViewModel.RegisterResult.Error -> {
                    Toast.makeText(this, result.errorMessage, Toast.LENGTH_SHORT).show()
                }

                else -> {
                    Toast.makeText(this, "未知错误", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 点击注册按钮触发注册逻辑
        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val repeatPassword = repeatPasswordEditText.text.toString().trim()

            registerViewModel.register(username, password, repeatPassword)
        }
    }

    private fun navigateToMainActivity() {
        // 导航到主界面
        val intent = Intent(this@RegisterActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
