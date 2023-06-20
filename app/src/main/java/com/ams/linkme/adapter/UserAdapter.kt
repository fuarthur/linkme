package com.ams.linkme.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ams.linkme.R
import com.ams.linkme.model.User

class UserAdapter(private val userList: List<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewUsername: TextView = itemView.findViewById(R.id.textViewUsername)
        val textViewEmail: TextView = itemView.findViewById(R.id.textViewEmail)
        val textViewGender: TextView = itemView.findViewById(R.id.textViewGender)
        val textViewPhone: TextView = itemView.findViewById(R.id.textViewPhone)
        val textViewInterests: TextView = itemView.findViewById(R.id.textViewInterests)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
        val context = holder.itemView.context

        holder.textViewUsername.text = context.getString(R.string.username_placeholder, currentUser.username)
        holder.textViewEmail.text = context.getString(R.string.email_placeholder, currentUser.email)
        holder.textViewGender.text = context.getString(R.string.gender_placeholder, currentUser.gender)
        holder.textViewPhone.text = context.getString(R.string.phone_placeholder, currentUser.phone)
        holder.textViewInterests.text =
            context.getString(R.string.interests_placeholder, currentUser.interests?.joinToString(", "))
    }


    override fun getItemCount(): Int {
        return userList.size
    }
}

