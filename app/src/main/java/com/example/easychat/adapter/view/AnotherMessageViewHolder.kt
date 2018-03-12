package com.example.easychat.adapter.view

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import com.bumptech.glide.Glide
import com.example.easychat.R
import com.example.easychat.models.ChatMessage
import kotlinx.android.synthetic.main.adapter_chat_another_message.view.*

class AnotherMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bindItem(context: Context, data: ChatMessage, isGroup: Boolean) {
        itemView.messageTextView.text = data.text
        if (isGroup) {
            itemView.nameTextView.visibility = View.GONE
            itemView.userImageView.visibility = View.INVISIBLE
        } else {
            itemView.userImageView.visibility = View.VISIBLE
            Glide.with(context)
                    .load(data.image)
                    .crossFade()
                    .placeholder(ContextCompat.getDrawable(context, R.drawable.ic_person_outline))
                    .into(itemView.userImageView)

            itemView.nameTextView.visibility = View.VISIBLE
            itemView.nameTextView.text = data.username
        }
    }
}
