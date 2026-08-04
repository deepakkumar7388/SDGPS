package com.example.digitalpass

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecentPassAdapter(var listType:String,var recentPassList:ArrayList<HashMap<String,String>>) :
    RecyclerView.Adapter<RecentPassAdapter.ViewHolder>() {

    var listTypeByDate="recent"
    var onItemClick: ((HashMap<String, String>) -> Unit)? = null

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView=itemView.findViewById<ImageView>(R.id.memberImage)
        var name=itemView.findViewById<TextView>(R.id.memberName)
        var status=itemView.findViewById<TextView>(R.id.memberRole)
        var applyDateTime=itemView.findViewById<TextView>(R.id.applyDateTime)
        var itemLayout=itemView.findViewById<View>(R.id.historyItemCompleteLayout)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecentPassAdapter.ViewHolder {
        var view= LayoutInflater.from(parent.context).inflate(R.layout.historyitem,parent,false)
        return ViewHolder(view)

    }

    override fun onBindViewHolder(holder: RecentPassAdapter.ViewHolder, position: Int) {
        holder.name.text=recentPassList[position]["name"]
        
        var statusVal = recentPassList[position]["status"]
        holder.status.text=statusVal
        
        when (statusVal?.lowercase()) {
            "approving" -> holder.status.setTextColor(android.graphics.Color.parseColor("#F39C12"))
            "approved" -> holder.status.setTextColor(android.graphics.Color.parseColor("#28A745"))
            "rejected" -> holder.status.setTextColor(android.graphics.Color.parseColor("#DC3545"))
            "exit" -> holder.status.setTextColor(android.graphics.Color.parseColor("#17A2B8"))
            "expired" -> holder.status.setTextColor(android.graphics.Color.parseColor("#795548"))
            else -> holder.status.setTextColor(android.graphics.Color.parseColor("#636E72")) // pending / default
        }

        //make status in uppercase
        holder.status.text=statusVal?.uppercase()
        if(statusVal=="approving")holder.status.text="In Process"
        

        holder.applyDateTime.visibility=View.VISIBLE
        if(listType=="visitor")holder.applyDateTime.text=recentPassList[position]["entryDate"]
        else holder.applyDateTime.text=recentPassList[position]["applyDate"]
        if(recentPassList[position]["img"]?.trim()!="")
            Glide.with(holder.imageView.context).load(LoginUserDataHolder.getURL(recentPassList[position]["img"])).into(holder.imageView)
        else{
            Glide.with(holder.imageView.context).load(R.drawable.user_icon).into(holder.imageView)
        }


        holder.itemLayout.setOnClickListener {
            if (onItemClick != null) {
                onItemClick?.invoke(recentPassList[position])
                return@setOnClickListener
            }

            if(listType=="visitor") {
                var intent = Intent(holder.itemView.context, EnterVisitor::class.java)
                var visitorHash=HashMap(recentPassList[position])
                intent.putExtra("visitor", visitorHash)
                intent.putExtra("operation", "edit")
                intent.putExtra("listType",listTypeByDate)
                holder.itemView.context.startActivity(intent)
            }
            else if(listType=="gatePass"){
                var intent=Intent(holder.itemView.context, GatePassDetail::class.java)
                intent.putExtra("gatePass",recentPassList[position])
                intent.putExtra("operationType","member")
                intent.putExtra("listType",listTypeByDate)
                holder.itemView.context.startActivity(intent)
            }
            else{
                var intent=Intent(holder.itemView.context, GatePassDetail::class.java)
                intent.putExtra("gatePass",recentPassList[position])
                intent.putExtra("operationType","self")
                intent.putExtra("listType",listTypeByDate)
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int {
        return recentPassList.size
    }

    fun updateList(newList:ArrayList<HashMap<String,String>>){
        recentPassList=newList
        notifyDataSetChanged()
    }


    fun updateItem(updatedVisitor:HashMap<String,String>){
        var position = if(listType == "visitor") {
            recentPassList.indexOfFirst { it["visitorId"] == updatedVisitor["visitorId"] }
        } else {
            recentPassList.indexOfFirst { it["gatePassId"] == updatedVisitor["gatePassId"] }
        }

        if(position==-1)return
        //replace item with updated visitor/gatePass
        recentPassList[position]=updatedVisitor
        notifyItemChanged(position)
    }
    fun insertItem(newItem: HashMap<String, String>) {
        val idKey = if (listType == "visitor") "visitorId" else "gatePassId"
        val existingIndex = recentPassList.indexOfFirst { it[idKey] == newItem[idKey] }
        if (existingIndex == -1) {
            // Item is genuinely new — insert at top and notify
            recentPassList.add(0, newItem)
            notifyItemInserted(0)
            notifyItemRangeChanged(0, recentPassList.size)
        } else {
            // Item already exists (duplicate socket event) — update it in-place
            // instead of crashing with an IndexOutOfBoundsException
            recentPassList[existingIndex] = newItem
            notifyItemChanged(existingIndex)
        }
    }


}