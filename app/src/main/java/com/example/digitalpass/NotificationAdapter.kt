package com.example.digitalpass

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.digitalpass.database.NotificationEntity
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
     var notifications: List<NotificationEntity>,
    private val onMarkAsReadClick: (NotificationEntity) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.notificationImage)
        val title: TextView = itemView.findViewById(R.id.notificationTitle)
        val name=itemView.findViewById<TextView>(R.id.notifyName)
        val body: TextView = itemView.findViewById(R.id.notificationBody)
        val time: TextView = itemView.findViewById(R.id.notificationTime)
        val markAsReadButton: MaterialButton = itemView.findViewById(R.id.markAsReadButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        
        holder.title.text = notification.title
        holder.name.text=notification.name
        holder.body.text = notification.body
        
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        holder.time.text = sdf.format(Date(notification.timestamp))

        Glide.with(holder.itemView.context)
            .load(LoginUserDataHolder.getURL(notification.imgUrl))
            .circleCrop()
            .placeholder(R.drawable.user_icon)
            .into(holder.image)

        holder.markAsReadButton.setOnClickListener {
            onMarkAsReadClick(notification)
        }
    }

    override fun getItemCount(): Int = notifications.size

    fun updateData(newNotifications: List<NotificationEntity>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }
}
