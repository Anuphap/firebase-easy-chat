package com.example.easychat.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.easychat.R
import com.example.easychat.adapter.view.AnotherMessageViewHolder
import com.example.easychat.adapter.view.MineMessageViewHolder
import com.example.easychat.models.ChatMessage
import com.google.firebase.auth.FirebaseUser

class MessageAdapter(private val context: Context, private var chatMessages: List<ChatMessage>, val user: FirebaseUser) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val ITEM_TYPE_ANOTHER: Int = 1
        private const val ITEM_TYPE_MINE: Int = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_TYPE_ANOTHER) {
            val anotherView = LayoutInflater.from(context).inflate(R.layout.adapter_chat_another_message, parent, false)
            AnotherMessageViewHolder(anotherView) // view holder for another text
        } else {
            val mineView = LayoutInflater.from(context).inflate(R.layout.adapter_chat_message, parent, false)
            MineMessageViewHolder(mineView) // view holder for mine text
        }
    }

    fun setMessages(chatMessages: List<ChatMessage>) {
        this.chatMessages = chatMessages
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (chatMessages[position].username == user.displayName) {
            ITEM_TYPE_MINE
        } else {
            ITEM_TYPE_ANOTHER
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        val data = chatMessages[position]
        val isGroup = if (position == 0) false else
            chatMessages[position - 1].username == chatMessages[position].username
        val itemType = getItemViewType(position)

        if (itemType == ITEM_TYPE_MINE) {
            (holder as MineMessageViewHolder).bindItem(data, isGroup)
        } else if (itemType == ITEM_TYPE_ANOTHER) {
            (holder as AnotherMessageViewHolder).bindItem(context, data, isGroup)
        }
    }

    override fun getItemCount(): Int = chatMessages.size
}