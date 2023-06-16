package com.ams.linkme

import android.app.Application
import android.content.Intent
import android.util.Log
import com.ams.linkme.ui.activity.LoginActivity
import com.google.firebase.FirebaseApp

class LinkMeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // 初始化其他配置或依赖项
        // 跳转到 LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

}
