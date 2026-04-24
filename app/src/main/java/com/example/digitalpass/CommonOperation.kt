package com.example.digitalpass

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.Glide
import com.example.digitalpass.LoginUserDataHolder.loginUserData
import com.example.digitalpass.LoginUserDataHolder.token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File

object CommonOperation {

    fun setupUserProfile(activity: Activity) {

        //we will do all this work with CoroutineScope
        CoroutineScope(Dispatchers.Main).launch {

            if (LoginUserDataHolder.loginUserData?.get("img")?.trim() != "") {
                var profileImage = activity.findViewById<ImageView>(R.id.ProfileImage)
                Glide.with(activity)
                    .load(LoginUserDataHolder.getURL(LoginUserDataHolder.loginUserData?.get("img")))
                    .into(profileImage)
                var navProfileImage = activity.findViewById<ImageView>(R.id.navProfileImage)
                Glide.with(activity)
                    .load(LoginUserDataHolder.getURL(LoginUserDataHolder.loginUserData?.get("img")))
                    .into(navProfileImage)
            }

            activity.findViewById<TextView>(R.id.profileName).text =LoginUserDataHolder.loginUserData?.get("name")
            activity.findViewById<TextView>(R.id.profileRole).text =LoginUserDataHolder.loginUserData?.get("role")
            activity.findViewById<TextView>(R.id.profileEmail).text =LoginUserDataHolder.loginUserData?.get("email")
            activity.findViewById<TextView>(R.id.profilePhone).text =LoginUserDataHolder.loginUserData?.get("phone")
            activity.findViewById<TextView>(R.id.profileCampus).text =LoginUserDataHolder.loginUserData?.get("campus")
            activity.findViewById<TextView>(R.id.profileDepartment).text =LoginUserDataHolder.loginUserData?.get("department")
            activity.findViewById<TextView>(R.id.profileBatch).text =LoginUserDataHolder.loginUserData?.get("batch")

            if(LoginUserDataHolder.loginUserData?.get("role")=="student"){
                activity.findViewById<LinearLayout>(R.id.studentLayout).visibility=LinearLayout.VISIBLE
                activity.findViewById<TextView>(R.id.studentUid).text =LoginUserDataHolder.loginUserData?.get("uid")
                activity.findViewById<TextView>(R.id.fatherName).text =LoginUserDataHolder.loginUserData?.get("fathername")
                activity.findViewById<TextView>(R.id.fatherPhone).text =LoginUserDataHolder.loginUserData?.get("fatherphone")
            }

            var logoutButton=activity.findViewById<com.google.android.material.button.MaterialButton>(R.id.logoutButton)
            logoutButton.setOnClickListener {
                logout(activity)
            }
        }

    }

     fun uploadImage(context:Activity,uri: Uri) {

        CoroutineScope(Dispatchers.IO).launch {
            try {

                //check the size of image upto 1500KB
                var size=context.contentResolver.openAssetFileDescriptor(uri,"r")?.length?:0
                if(size>1500000|| size.toInt() ==0){
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Image size should be less than 1.5MB", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                var imagePart =getMultipartImage(context, uri)

                val tokenRequestBody =
                    LoginUserDataHolder.token.toRequestBody("text/plain".toMediaTypeOrNull())

                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT)
                        .show()
                }
                val call = RetrofitClient.instance.uploadProfileImage(imagePart, tokenRequestBody)
                call.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody?>,
                        response: Response<ResponseBody?>
                    ) {
                        if (response.isSuccessful) {
                            Toast.makeText(
                                context,
                                "Image uploaded successfully",
                                Toast.LENGTH_SHORT
                            ).show()

                                //now update the profileImage and navProfileImage
                                Glide.with(context).load(uri)
                                    .into(context.findViewById(R.id.ProfileImage))
                                Glide.with(context).load(uri).into(context.findViewById(R.id.navProfileImage))
                        } else {
                            val errorMessage = LoginUserDataHolder.getErrorMessage(response)
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG)
                                .show()
                        }
                    }

                    override fun onFailure(
                        call: Call<ResponseBody?>,
                        t: Throwable
                    ) {
                        Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
                    }

                })

            } catch (e: Exception) {
                Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        }
    }


    //logOut the user by socket disconnecting,remove token and remove fcm token
    fun logout(context:Context) {
        //first we have to remove fcm token from database
        var callToLogout= RetrofitClient.instance.logout(token)
        callToLogout.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody?>,
                response: Response<ResponseBody?>
            ) {
                if(response.isSuccessful){
                    SocketManager.disconnect()
                    token=""
                    loginUserData=null
                    context.getSharedPreferences("DigitalPassPrefs", Context.MODE_PRIVATE).edit().clear().apply()
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Log out successfully", Toast.LENGTH_SHORT).show()
                        var intent = Intent(context, MainActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)

                        //now finish this context activity
                        if(context is Activity)context.finish()
                    }

                }
            }

            override fun onFailure(
                call: Call<ResponseBody?>,
                t: Throwable
            ) {
                TODO("Not yet implemented")
            }
        })

    }



    fun getMultipartImage(context: Context, uri: Uri): MultipartBody.Part{
        return try{
            val file=getCompressedBytes(context,uri)
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("img", "img.jpg", requestFile)
        }catch (e: Exception) {
            e.printStackTrace()
            throw IllegalArgumentException("Invalid image")
        }
    }

    fun getCompressedBytes(context: Context, uri: Uri): File {
        return try {
            // 1. Open stream once to read EXIF orientation
            val exifStream = context.contentResolver.openInputStream(uri)
            val orientation = exifStream?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            // 2. Open stream again to decode the Bitmap
            val bitmapStream = context.contentResolver.openInputStream(uri)
            val bitmap = bitmapStream.use { BitmapFactory.decodeStream(it) }
                ?: throw IllegalArgumentException("Could not decode bitmap")

            var quality = 100
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)

            // 3. Compression loop (This MUST run in CoroutineScope(Dispatchers.IO))
            while (stream.toByteArray().size / 1024 > 200 && quality > 10) {
                stream.reset()
                quality -= if (quality < 30) 5 else 10
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            }

            // 4. Save to cache file
            val file = File(context.cacheDir, "img_${System.currentTimeMillis()}.jpg")
            file.writeBytes(stream.toByteArray())

            // 5. Restore Orientation metadata to the new file
            val newExif = ExifInterface(file.absolutePath)
            newExif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            newExif.saveAttributes()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            throw e // Throw so getMultipartImage knows it failed
        }
    }
}