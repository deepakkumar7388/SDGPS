package com.example.digitalpass

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import java.io.ByteArrayOutputStream
import java.io.File

object LoginUserDataHolder {
    var loginUserData: HashMap<String, String>? = null
    lateinit var token: String

    var campusForBatchOperation=""

     fun storeFCMToken() {
        //get fcm token and store it
        FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
            var callToStoreFCMToken=RetrofitClient.instance.storeFCMToken(hashMapOf(
                "token" to token,
                "fcmToken" to fcmToken))
            callToStoreFCMToken.enqueue(object:Callback<ResponseBody>{
                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {

                }

                override fun onFailure(
                    call: Call<ResponseBody?>,
                    t: Throwable
                ) {
                    TODO("Not yet implemented")
                }
            })

        }

    }

    fun getErrorMessage(response: Response<*>):String{
        // When the response is not successful, the error message is in errorBody()
        val errorMessage = try {
            // Read the error body as a string. It might be a JSON object.
            val errorBodyString = response.errorBody()?.string()

            // If the error string is not null, try to parse it as JSON to get the "message"
            if (errorBodyString != null) {
                // Assuming the error is a JSON like {"message": "some error"}
                val errorJson = JSONObject(errorBodyString)
                errorJson.getString("message") // Extract the value of the "message" key
            } else {
                "An unknown error occurred" // Fallback message
            }
        } catch (e: Exception) {
            // If parsing fails or another error occurs, show a generic message
            "Error parsing response"
        }

        return errorMessage

    }

    fun getURL(img: String?):String{
        //Return the cloudinary URL by generating user img
        return "https://res.cloudinary.com/dtdo4gzfh/image/upload/$img.jpg?t=${System.currentTimeMillis()}"
    }


    var visitorListAdapter: RecentPassAdapter?=null
    var visitorList: ArrayList<HashMap<String, String>>? = null
    var gatePassListAdapter: RecentPassAdapter?=null
    var gatePassList: ArrayList<HashMap<String, String>>? = null

    fun getVisitorList(){
        //get visitor list
        val call = RetrofitClient.instance.getRecentVisitorList(LoginUserDataHolder.token)
        call.enqueue(object : Callback<ArrayList<HashMap<String, String>>> {
            override fun onResponse(
                call: Call<ArrayList<HashMap<String, String>>?>,
                response: Response<ArrayList<HashMap<String, String>>?>
            ) {
                if (response.isSuccessful) {
                    visitorList = response.body()
                    //we will update visitorListAdapter with copy of visitorList
                    visitorListAdapter?.updateList(visitorList?.toMutableList() as ArrayList<HashMap<String, String>>)
                }

            }
            override fun onFailure(
                call: Call<ArrayList<HashMap<String, String>>?>,
                t: Throwable
            ) {
                Log.d("TAG", "onFailure: ${t.message}")
            }

        })
    }


    fun updateVisitorStatus(data:JSONObject){
        CoroutineScope(Dispatchers.Main).launch {
            //set the status of visitor with this visitorId
            var position=visitorList?.indexOfFirst { it["visitorId"]==data.getString("visitorId") }
            if(position!=-1&&position!=null){

                visitorList!![position]["status"]=data.getString("operation")
                filterAndUpdateItem(visitorList!![position])
            }
        }
    }

    fun updatedVisitor(operation:String,visitorId:String){
        //get the visitor with this visitorId
        var callToGetVisitor= RetrofitClient.instance.getRecentUpdatedVisitor(hashMapOf(
            "token" to token,
            "visitorId" to visitorId
        ))
        callToGetVisitor.enqueue(object : Callback<HashMap<String, String>> {
            override fun onResponse(
                call: Call<HashMap<String, String>?>,
                response: Response<HashMap<String, String>?>
            ) {
                if(response.isSuccessful){
                    var updatedVisitor=response.body()!!

                    CoroutineScope(Dispatchers.Main).launch {
                        if (operation == "insert") {
                            visitorList?.add(0,updatedVisitor)
                            if(listType=="visitor"&&updatedVisitor["name"]!!.contains(searchQuery,ignoreCase = true)){
                                visitorListAdapter?.insertItem(updatedVisitor)
                            }
                        } else {
                            var position=visitorList?.indexOfFirst { it["visitorId"]==updatedVisitor["visitorId"] }
                            if(position!=-1&&position!=null){
                                visitorList!![position]=updatedVisitor
                                filterAndUpdateItem(updatedVisitor)
                            }
                        }
                    }

                }
            }

            override fun onFailure(
                call: Call<HashMap<String, String>?>,
                t: Throwable
            ) {
                TODO("Not yet implemented")
            }

        })


    }

    var searchQuery=""
    var listType="visitor"


    fun filterList(query:String){

        searchQuery=query
        if(listType=="visitor")visitorListAdapter?.updateList(visitorList?.filter { it["name"]?.contains(query,ignoreCase = true)?:false } as ArrayList<HashMap<String, String>>)
        else gatePassListAdapter?.updateList(gatePassList?.filter { it["name"]?.contains(query,ignoreCase = true)?:false } as ArrayList<HashMap<String, String>>)

    }

    fun filterAndUpdateItem(updatedItem:HashMap<String,String>){
        if(listType=="visitor"){
            //also we have to filter this user on the basis of searchQuery
            if(updatedItem["name"]?.contains(searchQuery,ignoreCase = true)?:false)visitorListAdapter?.updateItem(updatedItem)
        }
        else{
            if(updatedItem["name"]?.contains(searchQuery,ignoreCase = true)?:false)gatePassListAdapter?.updateItem(updatedItem)
        }
    }



    fun getGatePassList() {
        //get gate pass list
        val call = RetrofitClient.instance.getRecentGatePassList(token)
        call.enqueue(object : Callback<ArrayList<HashMap<String, String>>> {
            override fun onResponse(
                call: Call<ArrayList<HashMap<String, String>>?>,
                response: Response<ArrayList<HashMap<String, String>>?>
            ) {
                if (response.isSuccessful) {
                    gatePassList = response.body()
                    //update copy of gatePassList
                    gatePassListAdapter?.updateList(gatePassList?.toMutableList() as ArrayList<HashMap<String, String>>)
                }
            }

            override fun onFailure(
                call: Call<ArrayList<HashMap<String, String>>?>,
                t: Throwable
            ) {
                TODO("Not yet implemented")
            }
        })
    }

    fun insertNewGatePass(gatePassId:String){
        //now get the new insert gate pass
        var callToGetGatePass= RetrofitClient.instance.getRecentUpdatedGatePass(hashMapOf(
            "token" to token,
            "gatePassId" to gatePassId
        ))
        callToGetGatePass.enqueue(object : Callback<HashMap<String, String>> {
            override fun onResponse(
                call: Call<HashMap<String, String>?>,
                response: Response<HashMap<String, String>?>
            ) {
                if(response.isSuccessful){
                    var newGatePass=response.body()!!
                    gatePassList?.add(0,newGatePass)

                    //before inserting this new gatePass in adapter we have to check for searchQuery
                    if(listType=="gatePass"&&newGatePass["name"]!!.contains(searchQuery,ignoreCase = true)){
                        gatePassListAdapter?.insertItem(newGatePass)
                    }
                }
            }

            override fun onFailure(
                call: Call<HashMap<String, String>?>,
                t: Throwable
            ) {
                TODO("Not yet implemented")
            }
        })
    }
    fun updateGatePass(operation:String,data:JSONObject) {
        CoroutineScope(Dispatchers.Main).launch {

            //first we will get position of gatePass with this data["gatePassId"]
            var position=gatePassList?.indexOfFirst { it["gatePassId"]==data.getString("gatePassId") }

            if(position!=-1&&position!=null){
            if(operation=="updateRemark"){
                    //then check if there is any key in data like reason or tgRemark
                    if(data.has("reason"))gatePassList!![position]["reason"]=data.getString("reason")
                    if(data.has("tgRemark"))gatePassList!![position]["tgRemark"]=data.getString("tgRemark")
                }
                else{
                    if(data.has("status"))gatePassList!![position]["status"]=data.getString("status")
            }
                //update this gatePass in gatePassList
                filterAndUpdateItem(gatePassList!![position])
            }
        }
    }

}