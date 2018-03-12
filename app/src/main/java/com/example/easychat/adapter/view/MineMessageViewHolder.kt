package com.example.easychat.adapter.view

import android.support.v7.widget.RecyclerView
import android.view.View
import com.example.easychat.models.ChatMessage
import kotlinx.android.synthetic.main.adapter_chat_message.view.*

class MineMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bindItem(data: ChatMessage, isGroup: Boolean) {
        itemView.titleTextView.visibility = if (isGroup) View.GONE else View.VISIBLE
        itemView.messageTextView.text = data.text
    }
}
