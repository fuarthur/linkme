package com.ams.linkme.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ams.linkme.R
import com.ams.linkme.model.Message
import com.google.firebase.auth.FirebaseAuth

class MessageAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val SENT_MESSAGE = 1
    private val RECEIVED_MESSAGE = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        // 根据消息的发送者来选择使用哪个布局文件
        val layoutId = if (viewType == SENT_MESSAGE) {
            R.layout.item_message_sent
        } else {
            R.layout.item_message_received
        }

        val view = inflater.inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        val sender = message.senderId
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

        return if (sender == currentUserUid) {
            SENT_MESSAGE
        } else {
            RECEIVED_MESSAGE
        }
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bubbleTextView: TextView = itemView.findViewById(R.id.messageText)

        fun bind(message: Message) {
            bubbleTextView.text = message.content
        }
    }
}
