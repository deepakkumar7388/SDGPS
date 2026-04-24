package com.example.digitalpass

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.contextaware.ContextAware
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlin.text.trim

class LevelAdapter(var levelData: ArrayList<HashMap<String, String>>) :
    RecyclerView.Adapter<LevelAdapter.ViewHolder >(){

        var alternativeAdapter:LevelAdapter?=null
    var adapterForVisitor=false

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var levelImage=itemView.findViewById<ImageView>(R.id.memberImage)
            var levelName=itemView.findViewById<TextView>(R.id.memberName)
            var levelDepartment=itemView.findViewById<TextView>(R.id.memberRole)
            var itemLayout=itemView.findViewById<View>(R.id.historyItemCompleteLayout)
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LevelAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.historyitem, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: LevelAdapter.ViewHolder, position: Int) {
        if(levelData[position]["img"]?.trim()!="") Glide.with(holder.levelImage.context).load(
            LoginUserDataHolder.getURL(levelData[position]["img"])).into(holder.levelImage)
        else{
            Glide.with(holder.levelImage.context).clear(holder.levelImage)
            holder.levelImage.setImageResource(R.drawable.user_icon)
        }
        holder.levelName.text=levelData[position]["name"]
        holder.levelDepartment.text=levelData[position]["department"]

        //member add to alternative adapter
        holder.itemLayout.setOnClickListener {
            if(alternativeAdapter!=null) {
                alternativeAdapter!!.addNewItem(levelData[position])
                levelData.removeAt(position)
                notifyDataSetChanged()
            }

            //if this adapter is used for visitor
            if(adapterForVisitor){
                var intent= Intent(holder.itemView.context, MemberViewForVisitor::class.java)
                intent.putExtra("member",levelData[position])
                //we have to start activity for result
                val activity = holder.itemView.context as? android.app.Activity
                activity?.startActivityForResult(intent, 1)
            }
        }

    }

    override fun getItemCount(): Int {
        return levelData.size
    }

    //function to add new member to alternative adapter
    fun addNewItem(item:HashMap<String,String>){
        levelData.add(item)
        notifyDataSetChanged()
    }
    fun updateList(newList:ArrayList<HashMap<String,String>>){
        levelData=newList
        notifyDataSetChanged()
    }

}
