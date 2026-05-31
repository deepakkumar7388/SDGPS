package com.example.digitalpass

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Run the session guard BEFORE super.onCreate so nothing in the
        // subclass can touch LoginUserDataHolder while it is still empty.
        restoreSessionIfNeeded()
        super.onCreate(savedInstanceState)
    }

    private fun restoreSessionIfNeeded() {
        // loginUserData is null when the process was killed by the OS.
        if (LoginUserDataHolder.token.isEmpty() || LoginUserDataHolder.loginUserData == null || LoginUserDataHolder.loginUserData?.isEmpty() == true) {
            val restored = LoginUserDataHolder.loadState(this)
            if (!restored) {
                // Could not restore — send user back to splash/login.
                val intent = Intent(this, splashScreen::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                // Session restored successfully! We must reconnect the socket
                // to continue receiving live updates (if applicable for role).
                val role = LoginUserDataHolder.loginUserData?.get("role")
                if (role != null && role != "student") {
                    SocketManager.connect()
                }
            }
        }
    }
}
