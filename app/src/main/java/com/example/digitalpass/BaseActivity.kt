package com.example.digitalpass

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority


open class BaseActivity : AppCompatActivity() {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: ((Location?) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Run the session guard BEFORE super.onCreate so nothing in the
        // subclass can touch LoginUserDataHolder while it is still empty.
        restoreSessionIfNeeded()
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
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

    fun requestUserLocation(callback: (Location?) -> Unit) {
        this.locationCallback = callback
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        fusedLocationClient?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            ?.addOnSuccessListener { location ->
                locationCallback?.invoke(location)
                locationCallback = null
            }
            ?.addOnFailureListener {
                locationCallback?.invoke(null)
                locationCallback = null
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestUserLocation(locationCallback ?: return)
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
                locationCallback?.invoke(null)
                locationCallback = null
            }
        }
    }
}
