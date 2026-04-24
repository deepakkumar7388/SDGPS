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

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var imageView=itemView.findViewById<ImageView>(R.id.memberImage)
            var name=itemView.findViewById<TextView>(R.id.memberName)
            var status=itemView.findViewById<TextView>(R.id.memberRole)
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
        holder.status.text=recentPassList[position]["status"]
        if(recentPassList[position]["img"]?.trim()!="")
            Glide.with(holder.imageView.context).load(LoginUserDataHolder.getURL(recentPassList[position]["img"])).into(holder.imageView)

        holder.itemLayout.setOnClickListener {
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
        var position=recentPassList.indexOfFirst { it["visitorId"]==updatedVisitor["visitorId"] }

        if(position==-1)return
        //replace item with updated visitor
        recentPassList[position]=updatedVisitor
        notifyItemChanged(position)
    }
    fun insertItem(newVisitor:HashMap<String,String>){
        recentPassList.add(0,newVisitor)
        notifyItemInserted(0)
        notifyItemRangeChanged(0,recentPassList.size)
    }


}