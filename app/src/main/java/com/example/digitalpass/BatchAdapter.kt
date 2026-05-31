package com.example.digitalpass

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BatchAdapter(private var batchList: ArrayList<String>) :
    RecyclerView.Adapter<BatchAdapter.ViewHolder>(){

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
            var batchName=itemView.findViewById<TextView>(R.id.itemBatchName)
            var batchLayout=itemView.findViewById<View>(R.id.batchItemLayout)
        }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BatchAdapter.ViewHolder {
        var view= LayoutInflater.from(parent.context).inflate(R.layout.batchitem,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: BatchAdapter.ViewHolder, position: Int) {
        holder.batchName.text=batchList[position]

        holder.batchLayout.setOnClickListener {
            //navigate to level for batch
            var intent= Intent(holder.batchLayout.context, AddNewBatch::class.java)
            intent.putExtra("batchName",batchList[position])
            intent.putExtra("operation","edit")
            holder.batchLayout.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return batchList.size
    }

    fun updateList(newList:ArrayList<String>){
        batchList=newList
        notifyDataSetChanged()
    }

}