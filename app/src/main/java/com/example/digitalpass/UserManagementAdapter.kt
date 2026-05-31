package com.example.digitalpass

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class UserManagementAdapter(var members: ArrayList<HashMap<String,String>>) : RecyclerView.Adapter<UserManagementAdapter.MemberViewHolder>() {

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var image=itemView.findViewById<ImageView>(R.id.memberImage)
        var memberName: TextView = itemView.findViewById(R.id.memberName)
        var memberDepartment: TextView = itemView.findViewById(R.id.memberRole)
        var itemLayout: View = itemView.findViewById(R.id.historyItemCompleteLayout)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.historyitem, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        if(members[position]["img"]?.trim()!="") Glide.with(holder.image.context).load(
            LoginUserDataHolder.getURL(members[position]["img"])).into(holder.image)
        else{
            Glide.with(holder.image.context).clear(holder.image)
            holder.image.setImageResource(R.drawable.user_icon)
        }
        holder.memberName.text = members[position]["name"]
        holder.memberDepartment.text = members[position]["department"]
        holder.itemLayout.setOnClickListener {
            val intent = Intent(holder.itemView.context, UserManagementViewUser::class.java)
            intent.putExtra("user", members[position])
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return members.size
    }

    fun updateList(newList: ArrayList<HashMap<String,String>>?) {
        if (newList != null) {
            members = newList
        }
        notifyDataSetChanged()
    }


}