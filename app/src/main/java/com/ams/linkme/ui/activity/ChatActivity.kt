package com.ams.linkme.ui.activity

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ams.linkme.R
import com.ams.linkme.adapter.MessageAdapter
import com.ams.linkme.model.Message
import com.ams.linkme.ui.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ChatActivity : AppCompatActivity() {

    private lateinit var chatViewModel: ChatViewModel
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageQuery: Query
    private lateinit var childEventListener: ChildEventListener
    private lateinit var sendButton: Button
    private lateinit var messageEditText: EditText
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        setupRecyclerView()
        setupMessageQuery()
        setupMessageListener()

        sendButton = findViewById(R.id.button_send)
        messageEditText = findViewById(R.id.edit_text_message)

        sendButton.setOnClickListener {
            val messageText = messageEditText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    chatViewModel.sendMessage(messageText)
                    messageEditText.setText("")
                }
            }
        }

    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = messageAdapter
    }

    private fun setupMessageQuery() {
        val chatId = intent.getStringExtra("chatId") ?: ""
        val databaseReference = Firebase.database.reference.child("chats").child(chatId).child("messages")
        messageQuery = databaseReference.orderByChild("timestamp")
    }

    private fun setupMessageListener() {
        childEventListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    messageAdapter.addMessage(message)
                    recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        messageQuery.addChildEventListener(childEventListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        messageQuery.removeEventListener(childEventListener)
    }
}
