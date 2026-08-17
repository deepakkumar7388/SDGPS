package com.example.digitalpass

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.digitalpass.CommonOperation.logout

class ProfileActivity : ComponentActivity() {

    private val primaryColor = Color(0xFF052E92)
    private val currentImgUrl = mutableStateOf("")

    private val cropImageLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            if (uriContent != null) {
                currentImgUrl.value = uriContent.toString() // Instant UI update with local file
                CommonOperation.uploadImage(this, uriContent) {
                    // After server upload completes, update to the real Cloudinary URL with cache bust
                    val newImgKey = "profile_images/${LoginUserDataHolder.loginUserData?.get("email")}"
                    val cloudUrl = LoginUserDataHolder.getURL(newImgKey) + "?t=${System.currentTimeMillis()}"
                    runOnUiThread {
                        currentImgUrl.value = cloudUrl
                    }
                }
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                val options = CropImageContractOptions(uri, CropImageOptions(
                    imageSourceIncludeGallery = false,
                    imageSourceIncludeCamera = false,
                    aspectRatioX = 1,
                    aspectRatioY = 1,
                    fixAspectRatio = true,
                    cropMenuCropButtonTitle = "Done"
                ))
                cropImageLauncher.launch(options)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup initial user data mapping
        val user = LoginUserDataHolder.loginUserData
        val name = user?.get("name") ?: "Unknown User"
        val email = user?.get("email") ?: "No Email"
        val role = user?.get("role")?.replaceFirstChar { it.uppercase() } ?: "Unknown Role"
        val phone = user?.get("phone") ?: "N/A"
        val campus = user?.get("campus") ?: "N/A"
        val department = user?.get("department") ?: "N/A"
        val batch = user?.get("batch") ?: "N/A"
        val imgUrl = user?.get("img") ?: ""
        if (currentImgUrl.value.isEmpty()) {
            currentImgUrl.value = imgUrl
        }

        // Specific fields
        val studentUid = user?.get("uid") ?: "N/A"
        val fatherName = user?.get("fatherName") ?: user?.get("fathername")?.takeIf { it.isNotBlank() } ?: "N/A"
        val fatherPhone = user?.get("fatherPhone") ?: user?.get("fatherphone")?.takeIf { it.isNotBlank() } ?: "N/A"
        val isStudent = role.lowercase() == "student"

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = primaryColor,
                    background = Color(0xFFF8FAFC),
                    surface = Color.White
                )
            ) {
                ProfileScreen(
                    name = name,
                    email = email,
                    role = role,
                    phone = phone,
                    campus = campus,
                    department = department,
                    batch = batch,
                    imgUrl = currentImgUrl.value,
                    isStudent = isStudent,
                    studentUid = studentUid,
                    fatherName = fatherName,
                    fatherPhone = fatherPhone,
                    onBackClick = { finish() },
                    onEditPictureClick = {
                        val options = arrayOf("Camera", "Gallery")
                        android.app.AlertDialog.Builder(this@ProfileActivity)
                            .setTitle("Select Image Source")
                            .setItems(options) { _, which ->
                                if (which == 0) {
                                    // Camera - Use CanHub's direct camera launch
                                    val cropOptions = CropImageContractOptions(null, CropImageOptions(
                                        imageSourceIncludeGallery = false,
                                        imageSourceIncludeCamera = true,
                                        aspectRatioX = 1,
                                        aspectRatioY = 1,
                                        fixAspectRatio = true,
                                        cropMenuCropButtonTitle = "Done"
                                    ))
                                    cropImageLauncher.launch(cropOptions)
                                } else {
                                    // Gallery - Use System Gallery Picker
                                    val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                                    galleryLauncher.launch(intent)
                                }
                            }
                            .show()
                    },
                    onHistoryClick = {
                        startActivity(Intent(this@ProfileActivity, UserHistory::class.java))
                    },
                    onLogoutClick = {
                        logout(this@ProfileActivity, "thisUser")
                    }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ProfileScreen(
        name: String, email: String, role: String, phone: String,
        campus: String, department: String, batch: String, imgUrl: String,
        isStudent: Boolean, studentUid: String, fatherName: String, fatherPhone: String,
        onBackClick: () -> Unit,
        onEditPictureClick: () -> Unit,
        onHistoryClick: () -> Unit,
        onLogoutClick: () -> Unit
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Profile", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = primaryColor
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Profile Avatar with Edit Button
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(
                            model = if (imgUrl.isNotEmpty()) {
                                if (imgUrl.startsWith("content://") || imgUrl.startsWith("file://")) imgUrl
                                else LoginUserDataHolder.getURL(imgUrl)
                            } else R.drawable.user_icon,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.user_icon),
                            error = painterResource(id = R.drawable.user_icon),
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                        )
                        
                        // Edit Badge
                        IconButton(
                            onClick = onEditPictureClick,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(primaryColor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Picture",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text(text = email, fontSize = 14.sp, color = Color(0xFF64748B))
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    // Profile Details Card
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ProfileInfoRow(icon = R.drawable.ic_sidebar_role, title = "Role", value = role)
                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                            
                            ProfileInfoRow(icon = R.drawable.ic_sidebar_phone, title = "Phone Number", value = phone)
                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                            
                            ProfileInfoRow(icon = R.drawable.ic_sidebar_campus, title = "Campus", value = "$campus ($department)")
                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                            
                            ProfileInfoRow(icon = R.drawable.ic_sidebar_batch, title = "Batch", value = batch)

                            if (isStudent) {
                                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                                ProfileInfoRow(icon = R.drawable.ic_sidebar_uid, title = "Student UID", value = studentUid)
                                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                                ProfileInfoRow(icon = R.drawable.ic_sidebar_guardian, title = "Guardian Details", value = "$fatherName\n$fatherPhone")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    // History Menu Item
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onHistoryClick() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_nav_history_outline),
                                contentDescription = "History",
                                tint = primaryColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Pass / Visitor History",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Arrow Right",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    // Logout Button
                    OutlinedButton(
                        onClick = onLogoutClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    @Composable
    fun ProfileInfoRow(icon: Int, title: String, value: String) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = title,
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontSize = 12.sp, color = Color(0xFF94A3B8))
                Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
            }
        }
    }
}
