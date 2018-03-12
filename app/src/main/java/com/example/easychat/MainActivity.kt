package com.example.easychat

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.example.easychat.fragments.ChatFragment
import com.example.easychat.fragments.LoginFragment
import com.example.easychat.fragments.interfaces.SignInListener
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings


class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener,
        SignInListener, OnCompleteListener<Void> {

    private lateinit var auth: FirebaseAuth
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private lateinit var googleApiClient: GoogleApiClient
    private lateinit var loadingProgress: ProgressDialog
    private var logoutMenuView: MenuItem? = null

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_SIGN_IN: Int = 900
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // setup firebase auth
        auth = FirebaseAuth.getInstance()

        // setup firebase remote config
        remoteConfig = FirebaseRemoteConfig.getInstance()

        // config for debug
        val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build()

        remoteConfig.setConfigSettings(configSettings)
        remoteConfig.setDefaults(R.xml.remote_config_default)

        // setup view
        bindWidgetView()

        // setup Google login
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        googleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startChatFragment(currentUser)
        } else {
            startLoginFragment()
        }
    }

    override fun onResume() {
        super.onResume()
        fetchRemoteConfig()
    }

    override fun onPause() {
        super.onPause()
        if (googleApiClient.isConnecting) {
            googleApiClient.disconnect()
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == REQUEST_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)

            if (result.isSuccess) {
                // Google Sign In was successful, authenticate with Firebase
                loadingProgress.show()
                val account = result.signInAccount
                firebaseAuthWithGoogle(account!!)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        if (menu != null) {
            logoutMenuView = menu.getItem(0)
            if (auth.currentUser == null) {
                logoutMenuView!!.isVisible = false
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when {
                item.itemId == R.id.action_logout -> initLogout(item)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //region {@link addOnCompleteListener.onComplete} implementation
    override fun onComplete(task: Task<Void>) {
        if (task.isSuccessful) {
            remoteConfig.activateFetched() // active config
        }
        updateActionBar()
    }
    // end region

    private fun fetchRemoteConfig() {
        // cache config
        var cacheExpiration: Long = 3600 // 1 hour
        if (remoteConfig.info.configSettings.isDeveloperModeEnabled) {
            cacheExpiration = 0
        }
        remoteConfig.fetch(cacheExpiration).addOnCompleteListener(this)
    }

    private fun updateActionBar() {
        if (remoteConfig.getBoolean("is_display")) {
            supportActionBar?.title = remoteConfig.getString("app_name")
        }
    }

    //region {@link GoogleApiClient.OnConnectionFailedListener} implementation
    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d(TAG, "authWithGoogle: fail")
    }
    //end region

    //region {@link SignInListener.onRequestSignInListener} implementation
    override fun onRequestSignInListener() {
        signIn()
    }
    //end region

    private fun bindWidgetView() {
        // setup progress dialog
        loadingProgress = ProgressDialog(this)
        loadingProgress.setMessage(getString(R.string.loading))
        loadingProgress.setCancelable(false)
        loadingProgress.dismiss()
    }

    private fun signIn() {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
        startActivityForResult(signInIntent, REQUEST_SIGN_IN)
    }

    private fun initLogout(item: MenuItem?) {
        loadingProgress.show()

        if (item != null) {
            item.isEnabled = false
        }

        Auth.GoogleSignInApi.revokeAccess(googleApiClient).setResultCallback {
            FirebaseAuth.getInstance().signOut()
            startLoginFragment()
            loadingProgress.dismiss()
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithCredential:success")
                        val user = auth.currentUser

                        updateOrCreateUser(user)
                        startChatFragment(user)
                        loadingProgress.dismiss()
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        loadingProgress.dismiss()
                    }
                })
    }

    private fun updateOrCreateUser(user: FirebaseUser?) {
        val rootRef = FirebaseDatabase.getInstance().reference
        if (user != null) {
            val postValues = HashMap<String, Any>()
            postValues["username"] = user.displayName!!
            postValues["image"] = user.photoUrl.toString()

            val childUpdates = HashMap<String, Any>()
            childUpdates["/users/${user.uid}"] = postValues

            rootRef.updateChildren(childUpdates)
        }
    }

    private fun startLoginFragment() {
        if (logoutMenuView != null) {
            logoutMenuView!!.isVisible = false // invisible logout button
        }

        val fragment = LoginFragment()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.contentFrameLayout, fragment)
        transaction.commit()
    }

    private fun startChatFragment(user: FirebaseUser?) {
        if (logoutMenuView != null) {
            logoutMenuView!!.isEnabled = true
            logoutMenuView!!.isVisible = true // visible logout button
        }
        val fragment = ChatFragment().newInstance(user!!.photoUrl.toString())
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.contentFrameLayout, fragment)
        transaction.commit()
    }
}
