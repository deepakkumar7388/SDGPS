package com.example.digitalpass

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.Spannable
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.set
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.Glide
import com.example.digitalpass.LoginUserDataHolder.PREFS_NAME
import com.example.digitalpass.LoginUserDataHolder.loginUserData
import com.example.digitalpass.LoginUserDataHolder.token
import com.example.digitalpass.database.AppDatabase
import com.google.android.material.button.MaterialButton
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

    val versionId="7"
    var logoutButton: MaterialButton?=null
    fun setupUserProfile(activity: Activity) {

        //we will do all this work with CoroutineScope
        CoroutineScope(Dispatchers.Main).launch {

            activity.findViewById<ImageView>(R.id.navProfileImage).setOnClickListener {
                if (loginUserData?.get("img") != "") showFullScreenImage(
                    activity as Context,
                    "profile_images/${loginUserData?.get("email")}"
                )
            }
            //show app update if available
            val serverVersion = loginUserData?.get("versionId")
            if(serverVersion != versionId){
                val updateLayout = activity.findViewById<View>(R.id.updateBanner)
                updateLayout?.visibility = View.VISIBLE
                
                var updateButton=activity.findViewById<MaterialButton>(R.id.updateAppButton)
                    updateButton.setOnClickListener {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        if (!activity.packageManager.canRequestPackageInstalls()) {
                            val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                            intent.data = Uri.parse("package:" + activity.packageName)
                            activity.startActivity(intent)
                            Toast.makeText(activity, "Please allow 'Install Unknown Apps' and click Update again.", Toast.LENGTH_LONG).show()
                            return@setOnClickListener
                        }
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            androidx.core.app.ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                            Toast.makeText(activity, "Please allow notifications to see download progress and click Update again.", Toast.LENGTH_LONG).show()
                            return@setOnClickListener
                        }
                    }
                        updateButton.text="Updating..."
                        updateButton.isEnabled=false
                        activity.findViewById<ImageView>(R.id.laterButton).visibility=View.GONE

                    
                    val url = loginUserData?.get("downloadUrl")?.toString() ?: "https://github.com/yogeshsaini7172/sistecDigitalPassRelease/releases/latest/download/app-release.apk"
                    
                    val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager

                    val request = android.app.DownloadManager.Request(Uri.parse(url))
                        .setTitle("Digital Pass Update")
                        .setDescription("Downloading new version...")
                        .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setMimeType("application/vnd.android.package-archive")

                    val downloadId = downloadManager.enqueue(request)
                    
                    // Register broadcast receiver to auto install when done
                    val onComplete = object : android.content.BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            val id = intent?.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                            if (id == downloadId) {
                                val uri = downloadManager.getUriForDownloadedFile(downloadId)
                                if (uri != null && context != null) {
                                    val installIntent = Intent(Intent.ACTION_VIEW)
                                    installIntent.setDataAndType(uri, "application/vnd.android.package-archive")
                                    installIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    context.startActivity(installIntent)
                                }
                                try {
                                    context?.unregisterReceiver(this)
                                } catch (e: Exception) {
                                }
                            }
                        }
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        activity.registerReceiver(onComplete, android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
                    } else {
                        activity.registerReceiver(onComplete, android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE))
                    }
                    
                    Toast.makeText(activity, "Updating...", Toast.LENGTH_LONG).show()
                }
                
                activity.findViewById<ImageView>(R.id.laterButton)?.setOnClickListener {
                    updateLayout?.visibility = View.GONE
                }
            }

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

            //Digital Pass+campus
            var spannableText=android.text.SpannableString("SISTec Digital Pass\n${loginUserData?.get("campus")}")
            spannableText.setSpan(
                RelativeSizeSpan(1.2f),
                0,19,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spannableText.setSpan(
                RelativeSizeSpan(0.8f),
                19,spannableText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            activity.findViewById<TextView>(R.id.toolbarTitle).text=spannableText

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
            logoutButton=activity.findViewById<com.google.android.material.button.MaterialButton>(R.id.logoutButton)
            logoutButton?.setOnClickListener {
                val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(activity)
                val sheetView = activity.layoutInflater.inflate(R.layout.dialog_logout_bottom_sheet, null)
                bottomSheetDialog.setContentView(sheetView)

                val btnThisDevice = sheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnThisDevice)
                val btnAllDevices = sheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAllDevices)
                val btnCancel = sheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)

                btnThisDevice.setOnClickListener {
                    bottomSheetDialog.dismiss()
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
                        .setTitle("Confirm Logout")
                        .setMessage("Are you sure you want to logout from this device?")
                        .setPositiveButton("Yes, Logout") { _, _ ->
                            logout(activity, "thisUser")
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }

                btnAllDevices.setOnClickListener {
                    bottomSheetDialog.dismiss()
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
                        .setTitle("Confirm Logout")
                        .setMessage("Are you sure you want to logout from ALL devices? You will be signed out everywhere.")
                        .setPositiveButton("Yes, Logout All") { _, _ ->
                            logout(activity, "allUser")
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }

                btnCancel.setOnClickListener {
                    bottomSheetDialog.dismiss()
                }

                bottomSheetDialog.show()
            }
        }

    }

     fun uploadImage(context:Activity,uri: Uri) {

        CoroutineScope(Dispatchers.IO).launch {
            try {

                //check the size of image upto 1500KB
                var size=context.contentResolver.openAssetFileDescriptor(uri,"r")?.length?:0
                if(size>30720000|| size.toInt() ==0){
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Image size should be less than 30 MB", Toast.LENGTH_SHORT).show()
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
                                loginUserData?.put("img","profile_images/${loginUserData?.get("email")}")

                            //put this img in shared preference
                            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString("ud_img","profile_images/${loginUserData?.get("email")}").apply()

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
    fun logout(context:Context, logoutType: String) {
        logoutButton?.isEnabled=false
        val logoutData = HashMap<String, String>()
        logoutData["token"] = token
        logoutData["logoutType"] = logoutType
        //first we have to remove fcm token from database
        var callToLogout= RetrofitClient.instance.logout(logoutData)
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
                else{
                    Toast.makeText(context, LoginUserDataHolder.getErrorMessage(response), Toast.LENGTH_SHORT).show()
                }
                logoutButton?.isEnabled=true
            }

            override fun onFailure(
                call: Call<ResponseBody?>,
                t: Throwable
            ) {
                Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
                logoutButton?.isEnabled=true
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

    fun showFullScreenImage(context: Context, imageUrl: String?) {
        if (imageUrl.isNullOrBlank()) return
        val dialog = android.app.Dialog(context, R.style.FullScreenImageDialog)
        dialog.setContentView(R.layout.dialog_fullscreen_image)

        val fullScreenImageView = dialog.findViewById<ImageView>(R.id.fullScreenImageView)
        val closeButton = dialog.findViewById<ImageView>(R.id.closeButton)

        Glide.with(context)
            .load(LoginUserDataHolder.getURL(imageUrl))
            .into(fullScreenImageView)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.black)
        dialog.show()
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}
