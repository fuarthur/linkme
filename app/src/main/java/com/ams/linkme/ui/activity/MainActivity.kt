package com.ams.linkme.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        linkButton = findViewById(R.id.linkbutton)
        interestText = findViewById(R.id.searchEditText)
        linkButton.setOnClickListener {
            mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
            userList = mainViewModel.link(interestText.text.toString().trim())
            Log.d("UserList", userList.toString())
            // handleUserList(userList)
            bottomNavigate()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.bottom_navigation_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_link -> {
                Toast.makeText(this, "你已经在这一页了", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.menu_chat -> {
                navigateToChatActivity()
                true
            }

            R.id.menu_profile -> {
                navigateToProfileActivity()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleUserList(userList: List<User>) {
        // Access the userList and perform desired operations
        // For example, update the UI with the userList data
        val recyclerView: RecyclerView = findViewById(R.id.userRecyclerView)
        val adapter = UserAdapter(userList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
    fun bottomNavigate() {
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_link -> {
                    Toast.makeText(this, "你已经在这一页了", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_chat -> {
                    navigateToChatActivity()
                    true
                }
                R.id.menu_profile -> {
                    navigateToProfileActivity()
                    true
                }
                else -> false
            }
        }
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
