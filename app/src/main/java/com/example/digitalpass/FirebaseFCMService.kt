package com.example.digitalpass

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.NotificationTarget
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FirebaseFCMService: FirebaseMessagingService() {
    override fun onNewToken(fcmtoken: String) {
        val sharedPreferences = getSharedPreferences("DigitalPassPrefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)
        if (token != null) {
            val callToStoreToken = RetrofitClient.instance.storeFCMToken(hashMapOf(
                "token" to token,
                "fcmToken" to fcmtoken))
            callToStoreToken.enqueue(object: Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {}
                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {}
            })
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if(remoteMessage.data.isNotEmpty()){
            val data = remoteMessage.data
            
            var notificationId = System.currentTimeMillis().toInt()
            if(data.containsKey("visitorId")) {
                notificationId = data["visitorId"]?.toIntOrNull() ?: notificationId
            } else if (data.containsKey("gatePassId")) {
                notificationId = data["gatePassId"]?.toIntOrNull() ?: notificationId
            }



            // 2. Prepare RemoteViews
            val remoteView = RemoteViews(packageName, R.layout.custom_notification_layout)


                remoteView.setTextViewText(R.id.notificationTitle, data["title"])
                remoteView.setTextViewText(R.id.notificationDescriptionBody, data["body"])
                remoteView.setTextViewText(R.id.notificationUserName, data["name"])


            // 3. Setup Intent
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(this, notificationId, intent, PendingIntent.FLAG_IMMUTABLE)

            // create notification
            val notification = NotificationCompat.Builder(this, "DigitalPass")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setCustomContentView(remoteView)
                .setCustomBigContentView(remoteView)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            // 5. Post initial notification
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notification)

            // 6. Use Glide with overrides to prevent RemoteViews memory crash
            val target = NotificationTarget(this, R.id.notificationImage, remoteView, notification, notificationId)

            Glide.with(this)
                .asBitmap()
                .load(LoginUserDataHolder.getURL(data["img"]))
                .circleCrop()
                .override(150, 150) // CRITICAL: Small size for RemoteViews
                .placeholder(R.drawable.user_icon)
                .into(target)

        }
    }
}