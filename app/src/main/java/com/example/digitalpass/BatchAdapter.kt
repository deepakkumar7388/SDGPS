package com.example.digitalpass

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class BatchItemData(val name: String, val count: Int)

class BatchAdapter(private var batchList: ArrayList<BatchItemData>) :
    RecyclerView.Adapter<BatchAdapter.ViewHolder>(){

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
            var batchName=itemView.findViewById<TextView>(R.id.itemBatchName)
            var batchLayout=itemView.findViewById<View>(R.id.batchItemLayout)
            var memberCount=itemView.findViewById<TextView>(R.id.itemMemberCount)
            var membersButton=itemView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMembers)
        }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BatchAdapter.ViewHolder {
        var view= LayoutInflater.from(parent.context).inflate(R.layout.batchitem,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: BatchAdapter.ViewHolder, position: Int) {
        val batchItem = batchList[position]
        holder.batchName.text = batchItem.name
        holder.memberCount.text = "${batchItem.count} Members"

        holder.batchLayout.setOnClickListener {
            //navigate to level for batch
            var intent= Intent(holder.batchLayout.context, AddNewBatch::class.java)
            intent.putExtra("batchName", batchItem.name)
            intent.putExtra("operation", "edit")
            holder.batchLayout.context.startActivity(intent)
        }
        holder.membersButton.setOnClickListener {
            var intent= Intent(holder.membersButton.context, UserManagement::class.java)
            intent.putExtra("userManagementType","batch")
            intent.putExtra("batchName", batchItem.name)
            holder.membersButton.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return batchList.size
    }

    fun updateList(newList: ArrayList<BatchItemData>){
        batchList = newList
        notifyDataSetChanged()
    }
}