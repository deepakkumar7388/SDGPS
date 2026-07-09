package com.example.digitalpass

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
    var loginUserData: HashMap<String, String>? = HashMap()
    var token: String = ""

    var campusForBatchOperation=""


     const val PREFS_NAME = "DigitalPassPrefs"
    private const val KEY_TOKEN  = "token"
    private const val KEY_LAST_USER_SYNC_TIME = "lastUserSyncTime"
    private const val KEY_LAST_GATE_PASS_SYNC_TIME = "lastGatePassSyncTime"
    private const val KEY_LAST_VISITOR_SYNC_TIME = "lastVisitorSyncTime"
    // All loginUserData keys we want to persist:
    private val USER_DATA_KEYS = listOf(
        "name", "email", "phone", "role", "campus", "department",
        "batch", "img", "uid", "fathername", "fatherphone", "versionId"
    )


    fun saveState(context: Context) {
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        // Token is already saved by MainActivity / splashScreen, but we
        // also ensure it is written here for completeness.
        if (token.isNotEmpty()) editor.putString(KEY_TOKEN, token)
        loginUserData?.let { data ->
            for (key in USER_DATA_KEYS) {
                editor.putString("ud_$key", data[key] ?: "")
            }
        }
        editor.apply()
    }

    fun getLastUserSyncTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_USER_SYNC_TIME, 0L)
    }

    fun setLastUserSyncTime(context: Context, time: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_USER_SYNC_TIME, time).apply()
    }

    fun getLastGatePassSyncTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_GATE_PASS_SYNC_TIME, 0L)
    }

    fun setLastGatePassSyncTime(context: Context, time: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_GATE_PASS_SYNC_TIME, time).apply()
    }

    fun getLastVisitorSyncTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_VISITOR_SYNC_TIME, 0L)
    }

    fun setLastVisitorSyncTime(context: Context, time: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_VISITOR_SYNC_TIME, time).apply()
    }

    /**
     * Persists the campusForBatchOperation variable dynamically.
     */
    fun setCampusForBatch(context: Context, campus: String) {
        campusForBatchOperation = campus
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("campusForBatchOperation", campus).apply()
    }

    /**
     * Attempts to restore [loginUserData] and [token] from SharedPreferences.
     * Returns true on success, false if no valid session data was found.
     */
    fun loadState(context: Context): Boolean {
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedToken = prefs.getString(KEY_TOKEN, null) ?: return false
        token = savedToken
        val restoredData = HashMap<String, String>()
        for (key in USER_DATA_KEYS) {
            restoredData[key] = prefs.getString("ud_$key", "") ?: ""
        }
        campusForBatchOperation = prefs.getString("campusForBatchOperation", "") ?: ""
        loginUserData = restoredData
        return true
    }



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
                    Log.d("TAG", "storeFCMToken onFailure: ${t.message}")
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
        return "https://res.cloudinary.com/dtdo4gzfh/image/upload/$img.jpg"
    }


    var visitorListAdapter: RecentPassAdapter?=null
    var visitorList: ArrayList<HashMap<String, String>> = ArrayList()
    var gatePassListAdapter: RecentPassAdapter?=null
    var gatePassList: ArrayList<HashMap<String, String>> = ArrayList()

    fun getVisitorList(){
        //get visitor list
        val call = RetrofitClient.instance.getRecentVisitorList(LoginUserDataHolder.token)
        call.enqueue(object : Callback<ArrayList<HashMap<String, String>>> {
            override fun onResponse(
                call: Call<ArrayList<HashMap<String, String>>?>,
                response: Response<ArrayList<HashMap<String, String>>?>
            ) {
                if (response.isSuccessful) {
                    visitorList = response.body() ?: ArrayList()
                    //we will update visitorListAdapter with copy of visitorList
                    visitorListAdapter?.updateList(ArrayList(visitorList))
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
            val position = visitorList.indexOfFirst { it["visitorId"] == data.getString("visitorId") }
            if (position != -1) {
                visitorList[position]["status"] = data.getString("operation")
                filterAndUpdateItem(visitorList[position])
            }
        }
    }

    fun updatedVisitor(operation:String,visitorId:String){
        //get the visitor with this visitorId
        val callToGetVisitor = RetrofitClient.instance.getRecentUpdatedVisitor(hashMapOf(
            "token" to token,
            "visitorId" to visitorId
        ))
        callToGetVisitor.enqueue(object : Callback<HashMap<String, String>> {
            override fun onResponse(
                call: Call<HashMap<String, String>?>,
                response: Response<HashMap<String, String>?>
            ) {
                if(response.isSuccessful){
                    val body = response.body() ?: return
                    val updatedVisitor = body

                    CoroutineScope(Dispatchers.Main).launch {
                        if (operation == "insert") {
                            val position = visitorList.indexOfFirst { it["visitorId"] == updatedVisitor["visitorId"] }
                            if (position == -1) {
                                visitorList.add(0, updatedVisitor)
                                val nameStr = updatedVisitor["name"] ?: ""
                                if (listType == "visitor" && nameStr.contains(searchQuery, ignoreCase = true)) {
                                    visitorListAdapter?.insertItem(updatedVisitor)
                                }
                            }
                        } else {
                            val position = visitorList.indexOfFirst { it["visitorId"] == updatedVisitor["visitorId"] }
                            if (position != -1) {
                                visitorList[position] = updatedVisitor
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
            }

        })


    }

    var searchQuery=""
    var listType="visitor"


    fun filterList(query:String){

        searchQuery=query
        if(listType=="visitor") {
            val filtered = ArrayList(visitorList.filter { (it["name"] ?: "").contains(query, ignoreCase = true) })
            visitorListAdapter?.updateList(filtered)
        } else {
            val filtered = ArrayList(gatePassList.filter { (it["name"] ?: "").contains(query, ignoreCase = true) })
            gatePassListAdapter?.updateList(filtered)
        }

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



//    fun getGatePassList() {
//        //get gate pass list
//        val call = RetrofitClient.instance.getRecentGatePassList(token)
//        call.enqueue(object : Callback<ArrayList<HashMap<String, String>>> {
//            override fun onResponse(
//                call: Call<ArrayList<HashMap<String, String>>?>,
//                response: Response<ArrayList<HashMap<String, String>>?>
//            ) {
//                if (response.isSuccessful) {
//                    gatePassList = response.body() ?: ArrayList()
//                    //update copy of gatePassList
//                    gatePassListAdapter?.updateList(ArrayList(gatePassList))
//                }
//            }
//
//            override fun onFailure(
//                call: Call<ArrayList<HashMap<String, String>>?>,
//                t: Throwable
//            ) {
//                Log.d("TAG", "getGatePassList onFailure: ${t.message}")
//            }
//        })
//    }

    fun insertNewGatePass(gatePassId:String){
        //now get the new insert gate pass
        val callToGetGatePass = RetrofitClient.instance.getRecentUpdatedGatePass(hashMapOf(
            "token" to token,
            "gatePassId" to gatePassId
        ))
        callToGetGatePass.enqueue(object : Callback<HashMap<String, String>> {
            override fun onResponse(
                call: Call<HashMap<String, String>?>,
                response: Response<HashMap<String, String>?>
            ) {
                if(response.isSuccessful){
                    val newGatePass = response.body() ?: return
                    val position = gatePassList.indexOfFirst { it["gatePassId"] == newGatePass["gatePassId"] }
                    if (position == -1) {
                        gatePassList.add(0, newGatePass)

                        //before inserting this new gatePass in adapter we have to check for searchQuery
                        val nameStr = newGatePass["name"] ?: ""
                        if(listType=="gatePass" && nameStr.contains(searchQuery, ignoreCase = true)){
                            gatePassListAdapter?.insertItem(newGatePass)
                        }
                    }
                }
            }

            override fun onFailure(
                call: Call<HashMap<String, String>?>,
                t: Throwable
            ) {
                Log.d("TAG", "insertNewGatePass onFailure: ${t.message}")
            }
        })
    }
    fun updateGatePass(operation:String,data:JSONObject) {
        CoroutineScope(Dispatchers.Main).launch {

            //first we will get position of gatePass with this data["gatePassId"]
            val position = gatePassList.indexOfFirst { it["gatePassId"] == data.getString("gatePassId") }

            if(position != -1){
                if(operation=="updateRemark"){
                    //then check if there is any key in data like reason or tgRemark
                    if(data.has("reason")) gatePassList[position]["reason"] = data.getString("reason")
                    if(data.has("tgRemark")) gatePassList[position]["tgRemark"] = data.getString("tgRemark")
                }
                else{
                    if(data.has("status")) gatePassList[position]["status"] = data.getString("status")
                }
                //update this gatePass in gatePassList
                filterAndUpdateItem(gatePassList[position])
            }
        }
    }

}