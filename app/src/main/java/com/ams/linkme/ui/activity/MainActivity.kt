package com.ams.linkme.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ams.linkme.R
import com.ams.linkme.adapter.UserAdapter
import com.ams.linkme.model.User
import com.ams.linkme.ui.viewmodel.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var linkButton: Button
    private lateinit var interestText: EditText
    private lateinit var mainViewModel: MainViewModel
    private lateinit var userList: List<User>
    private lateinit var selectedItem: String
    private lateinit var btnProfile: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        linkButton = findViewById(R.id.linkbutton)
        interestText = findViewById(R.id.searchEditText)
        btnProfile = findViewById(R.id.btnProfile)
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        val spinner: Spinner = findViewById(R.id.spinner)
        val options = arrayOf("Sport", "Food", "Dating","Games","Music","Anime","Other") // 替换为你的选项列表
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedItem = parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 当没有选中项时的操作
            }
        }
        linkButton.setOnClickListener {
            mainViewModel.link(selectedItem, object : MainViewModel.LinkCallback {
                override fun onLinkResult(userList: List<User>) {
                    handleUserList(userList)
                    // Log.d("debug-annotation", userList.toString())
                }
            })
        }

        btnProfile.setOnClickListener {
            navigateToProfileActivity()
        }


    }


    private fun handleUserList(userList: List<User>) {
        // 获取RecyclerView视图组件
        val recyclerView: RecyclerView = findViewById(R.id.userRecyclerView)

        // 创建并设置适配器
        val adapter = UserAdapter(userList)
    }


    private fun navigateToChatActivity() {
        val intent = Intent(this@MainActivity, ChatActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToProfileActivity() {
        val intent = Intent(this@MainActivity, ProfileActivity::class.java)
        startActivity(intent)
        finish()
    }
}

