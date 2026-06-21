package com.example.digitalpass

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class UserManagementAdapter(
    var members: ArrayList<HashMap<String,String>>,
    private val onSelectionChange: (Int) -> Unit,
    var userItemClick:(HashMap<String,String>)->Unit
) : RecyclerView.Adapter<UserManagementAdapter.MemberViewHolder>() {

    val selectedItems = mutableSetOf<Int>()
    var isSelectionMode = false

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var image=itemView.findViewById<ImageView>(R.id.memberImage)
        var memberName: TextView = itemView.findViewById(R.id.memberName)
        var memberDepartment: TextView = itemView.findViewById(R.id.memberRole)
        var itemLayout: com.google.android.material.card.MaterialCardView = itemView.findViewById(R.id.historyItemCompleteLayout)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.historyitem, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val currentMember = members[position]
        if(currentMember["img"]?.trim()!="") Glide.with(holder.image.context).load(
            LoginUserDataHolder.getURL(currentMember["img"])).into(holder.image)
        else{
            Glide.with(holder.image.context).clear(holder.image)
            holder.image.setImageResource(R.drawable.user_icon)
        }
        holder.memberName.text = currentMember["name"]
        holder.memberDepartment.text = currentMember["department"]

        if (selectedItems.contains(position)) {
            holder.itemLayout.setCardBackgroundColor(android.graphics.Color.parseColor("#E0F2FE"))
            holder.itemLayout.strokeColor = android.graphics.Color.parseColor("#3B82F6")
            holder.itemLayout.strokeWidth = 4
        } else {
            holder.itemLayout.setCardBackgroundColor(android.graphics.Color.WHITE)
            holder.itemLayout.strokeWidth = 0
            holder.itemLayout.strokeColor = android.graphics.Color.TRANSPARENT
        }

        holder.itemLayout.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(position)
            } else {
                userItemClick(currentMember)
            }
        }

        holder.itemLayout.setOnLongClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
                toggleSelection(position)
            }
            true
        }
    }

    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }
        
        if (selectedItems.isEmpty()) {
            isSelectionMode = false
        }
        
        notifyItemChanged(position)
        onSelectionChange(selectedItems.size)
    }

    fun getSelectedUsers(): List<HashMap<String, String>> {
        return selectedItems.map { members[it] }
    }

    fun clearSelection() {
        selectedItems.clear()
        isSelectionMode = false
        notifyDataSetChanged()
        onSelectionChange(0)
    }

    override fun getItemCount(): Int {
        return members.size
    }

    fun updateList(newList: ArrayList<HashMap<String,String>>?) {
        clearSelection()
        if (newList != null) {
            members = newList
        }
        notifyDataSetChanged()
    }


}