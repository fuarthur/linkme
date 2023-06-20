package com.ams.linkme.ui.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ams.linkme.R
import com.ams.linkme.adapter.MessageAdapter
import com.ams.linkme.model.Message
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ChatActivity : AppCompatActivity() {

    private lateinit var currentUserId: String
    private lateinit var targetUserId: String
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var editTextMessage: EditText
    private lateinit var sendButton: Button
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        recyclerView = findViewById(R.id.recycler_view_chat)
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser!!.uid
        targetUserId = intent.getStringExtra("userId").toString()
        firestore = Firebase.firestore
        editTextMessage = findViewById(R.id.edit_text_message)
        sendButton = findViewById(R.id.button_send)
        sendButton.setOnClickListener {
            val content = editTextMessage.text.toString()
            if (content != "") {
                send(content)
            }
        }
        updateUI()
    }

    private fun send(content: String) {
        val postMap = hashMapOf<String, Any>()
        postMap["content"] = content
        postMap["senderId"] = currentUserId
        postMap["receiveId"] = targetUserId
        postMap["timestamp"] = com.google.firebase.Timestamp.now()
        firestore.collection("Messages").add(postMap).addOnSuccessListener { documentReference ->
            val delayMillis = 100
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                updateUI()
                editTextMessage.text.clear()
            }, delayMillis.toLong())
        }.addOnFailureListener {
            Toast.makeText(this, "Unknown error", Toast.LENGTH_LONG).show()
        }
    }


    private fun updateUI() {
        recyclerView.layoutManager = LinearLayoutManager(this)

        val query1 = firestore.collection("Messages")
            .whereEqualTo("senderId", currentUserId)
            .whereEqualTo("receiveId", targetUserId)
            .orderBy("timestamp")
            .get()

        val query2 = firestore.collection("Messages")
            .whereEqualTo("senderId", targetUserId)
            .whereEqualTo("receiveId", currentUserId)
            .orderBy("timestamp")
            .get()

        val combinedQuery = Tasks.whenAllSuccess<QuerySnapshot>(query1, query2)
        combinedQuery.addOnSuccessListener { taskSnapshots ->

            val messages = mutableListOf<Message>()

            for (snapshot in taskSnapshots) {
                val querySnapshot = snapshot as QuerySnapshot

                for (document in querySnapshot) {
                    val content = document.getString("content")
                    val senderId = document.getString("senderId")
                    val timestamp = document.getTimestamp("timestamp")?.toDate()?.time
                    val message = Message(targetUserId, senderId, content, timestamp)
                    messages.add(message)
                }
            }

            // 对消息列表按照时间戳进行排序
            messages.sortBy { it.timestamp }

            // 获取RecyclerView视图组件
            val recyclerView: RecyclerView = findViewById(R.id.recycler_view_chat)

            // 创建并设置适配器
            val adapter = MessageAdapter(messages)
            recyclerView.adapter = adapter

            // 滚动到最后一条消息
            recyclerView.scrollToPosition(messages.size - 1)
        }

        combinedQuery.addOnFailureListener { exception ->
            Toast.makeText(this, "Failed to fetch messages: ${exception.message}", Toast.LENGTH_SHORT).show()
        }

    }

}

