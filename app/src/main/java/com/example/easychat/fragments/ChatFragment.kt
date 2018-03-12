package com.example.easychat.fragments

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.example.easychat.R
import com.example.easychat.adapter.MessageAdapter
import com.example.easychat.models.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.fragment_chat.*


class ChatFragment : Fragment(), ValueEventListener, View.OnFocusChangeListener {

    private var user: FirebaseUser = FirebaseAuth.getInstance().currentUser as FirebaseUser
    private lateinit var name: String
    private lateinit var rootRef: DatabaseReference
    private lateinit var messageRef: DatabaseReference
    private lateinit var chatAdapter: MessageAdapter
    private var valueEventListener: ValueEventListener = this@ChatFragment

    companion object {
        private const val ARG_USER: String = "user"
    }

    fun newInstance(userId: String): ChatFragment {
        val args = Bundle()
        args.putString(ARG_USER, userId)
        val fragment = ChatFragment()
        fragment.arguments = args
        return fragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            name = arguments.getString(ARG_USER)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater!!.inflate(R.layout.fragment_chat, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        Toast.makeText(context, name, Toast.LENGTH_LONG).show()
        rootRef = FirebaseDatabase.getInstance().reference
        messageRef = rootRef.child("messages")
        prepareView()
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (v != null) {
            when {
                v.id == R.id.textInput -> chatRecyclerView.scrollToPosition(
                        if (chatAdapter.itemCount > 0) chatAdapter.itemCount - 1 else 0) // update scrolling to bottom
            }
        }
    }

    override fun onResume() {
        super.onResume()
        messageProgressBar.visibility = View.VISIBLE
        rootRef.child("messages").addValueEventListener(this@ChatFragment) //setup on update chat
    }

    override fun onDataChange(snapshot: DataSnapshot?) {
        if (snapshot != null) {
            val messages = ArrayList<ChatMessage>()
            snapshot.children.mapTo(messages) { it.getValue(ChatMessage::class.java) as ChatMessage }
            chatAdapter.setMessages(messages)
            if (messageProgressBar != null) {
                messageProgressBar.visibility = View.GONE
            }

            chatRecyclerView.scrollToPosition(messages.size - 1) // update scrolling to bottom
        }
    }

    override fun onCancelled(databaseError: DatabaseError?) {
        if (databaseError != null) {
            Log.d("Get chats: ", databaseError.message)
        }
    }

    private fun prepareView() {
        sendButton.setOnClickListener { sendMessage() }
        chatAdapter = MessageAdapter(context, ArrayList<ChatMessage>() as List<ChatMessage>, user)
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        layoutManager.stackFromEnd = true
        chatRecyclerView.layoutManager = layoutManager
        chatRecyclerView.adapter = chatAdapter
        textInput.onFocusChangeListener = this@ChatFragment
    }

    private fun sendMessage() {
//        val chatMessage = ChatMessage(textInput.text.toString(), user.displayName)
//        messageRef.push().setValue(chatMessage)

        val key = messageRef.push().key
        val postValues = HashMap<String, Any>()
        postValues["username"] = user.displayName!!
        postValues["text"] = textInput.text.toString()
        postValues["image"] = user.photoUrl.toString()

        val childUpdates = HashMap<String, Any>()
        childUpdates["/messages/$key"] = postValues
        childUpdates["/user-messages/${user.displayName}/$key"] = postValues

        rootRef.updateChildren(childUpdates)

        textInput.text = null
        hideKeyboard()
    }

    // region hide keyboard
    private fun hideKeyboard() {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity.currentFocus ?: return
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
    // endregion

    override fun onPause() {
        super.onPause()
        rootRef.removeEventListener(valueEventListener)
    }
}