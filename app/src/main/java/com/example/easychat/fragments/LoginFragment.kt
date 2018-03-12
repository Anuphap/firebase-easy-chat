package com.example.easychat.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.easychat.R
import com.example.easychat.fragments.interfaces.SignInListener
import kotlinx.android.synthetic.main.fragment_login.*

class LoginFragment : Fragment() {
    private var listener: SignInListener? = null
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater!!.inflate(R.layout.fragment_login, container, false)

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        this.listener = context as SignInListener
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        signInButton.setOnClickListener {
            listener?.onRequestSignInListener()
        }
    }
}