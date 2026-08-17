package com.example.digitalpass.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.digitalpass.LoginUserDataHolder

@Composable
fun UserManagementViewUserScreen(
    user: Map<String, String>,
    isLoading: Boolean,
    isEditMode: Boolean,
    onBack: () -> Unit,
    onToggleEditMode: () -> Unit,
    onSaveEdit: (updatedFields: Map<String, String>) -> Unit,
    onRemoveUser: () -> Unit
) {
    val context = LocalContext.current
    var nameText by remember(user) { mutableStateOf(user["name"] ?: "") }
    var emailText by remember(user) { mutableStateOf(user["email"] ?: "") }
    var phoneText by remember(user) { mutableStateOf(user["phone"] ?: "") }
    var departmentText by remember(user) { mutableStateOf(user["department"] ?: "") }
    var roleText by remember(user) { mutableStateOf(user["role"] ?: "") }
    var batchText by remember(user) { mutableStateOf(user["batch"] ?: "") }
    var uidText by remember(user) { mutableStateOf(user["uid"] ?: "") }
    var fatherNameText by remember(user) { mutableStateOf(user["fathername"] ?: "") }
    var fatherPhoneText by remember(user) { mutableStateOf(user["fatherphone"] ?: "") }

    var showImageDialog by remember { mutableStateOf(false) }

    val role = (user["role"] ?: "User").lowercase()
    val isStudent = role == "student"
    val imgUrl = user["img"] ?: user["profilePic"] ?: user["imageUrl"]

    val roleColor = when {
        isStudent -> Color(0xFF2563EB)
        role in listOf("hod", "principal", "admin") -> Color(0xFF7C3AED)
        role in listOf("faculty", "teacher", "tg") -> Color(0xFF059669)
        role == "security guard" -> Color(0xFFD97706)
        else -> Color(0xFF475569)
    }

    val loggedInRole = LoginUserDataHolder.loginUserData?.get("role")?.lowercase()?.trim() ?: ""
    val loggedInDept = LoginUserDataHolder.loginUserData?.get("department")?.trim() ?: ""
    val targetUserDept = user["department"]?.trim() ?: ""
    val targetRole = user["role"]?.lowercase()?.trim() ?: ""

    val canEditOrDelete = when (loggedInRole) {
        "admin", "principal" -> true
        "hod" -> loggedInDept.equals(targetUserDept, ignoreCase = true) && targetRole in listOf("faculty", "student", "reception", "security guard", "tg", "teacher")
        "faculty" -> loggedInDept.equals(targetUserDept, ignoreCase = true) && targetRole == "student"
        else -> false
    }

    // High-Res Image Zoom Modal for Identity Verification
    if (showImageDialog && !imgUrl.isNullOrBlank()) {
        Dialog(
            onDismissRequest = { showImageDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                // Close button top-right
                IconButton(
                    onClick = { showImageDialog = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(24.dp),
                        elevation = 12.dp,
                        color = Color.Black
                    ) {
                        AsyncImage(
                            model = LoginUserDataHolder.getURL(imgUrl),
                            contentDescription = "Full User Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = nameText,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${departmentText} • ${roleText.uppercase()}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    Scaffold(
        backgroundColor = Color(0xFFF4F6F9),
        topBar = {
            Surface(
                color = Color.White,
                elevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = Color(0xFFF1F5F9)
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF1E293B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = if (isEditMode) "Edit Member Details" else "User Details",
                            color = Color(0xFF0F172A),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (canEditOrDelete) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = if (isEditMode) Color(0xFFFEE2E2) else Color(0xFFEFF6FF)
                        ) {
                            IconButton(onClick = onToggleEditMode) {
                                Icon(
                                    imageVector = if (isEditMode) Icons.Default.Close else Icons.Outlined.Edit,
                                    contentDescription = "Edit",
                                    tint = if (isEditMode) Color(0xFFDC2626) else ThemeLogoBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (canEditOrDelete) {
                Surface(
                    color = Color.White,
                    elevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        if (isEditMode) {
                            Button(
                                onClick = {
                                    val updated = mutableMapOf<String, String>()
                                    if (nameText != (user["name"] ?: "")) updated["name"] = nameText
                                    if (emailText != (user["email"] ?: "")) updated["email"] = emailText
                                    if (phoneText != (user["phone"] ?: "")) updated["phone"] = phoneText
                                    if (departmentText != (user["department"] ?: "")) updated["department"] = departmentText
                                    if (roleText != (user["role"] ?: "")) updated["role"] = roleText
                                    if (batchText != (user["batch"] ?: "")) updated["batch"] = batchText
                                    if (uidText != (user["uid"] ?: "")) updated["uid"] = uidText
                                    if (fatherNameText != (user["fathername"] ?: "")) updated["fathername"] = fatherNameText
                                    if (fatherPhoneText != (user["fatherphone"] ?: "")) updated["fatherphone"] = fatherPhoneText
                                    onSaveEdit(updated)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = ThemeLogoBlue),
                                elevation = ButtonDefaults.elevation(4.dp, 8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Changes", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onRemoveUser,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFEE2E2)),
                                elevation = ButtonDefaults.elevation(0.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Remove User", color = Color(0xFFDC2626), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Sleek Profile Header Card with Zoomable Photo
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        backgroundColor = Color.White,
                        elevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar with Zoom Badge
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                                    .border(3.dp, roleColor.copy(alpha = 0.4f), CircleShape)
                                    .clickable {
                                        if (!imgUrl.isNullOrBlank()) showImageDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!imgUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = LoginUserDataHolder.getURL(imgUrl),
                                        contentDescription = "User Photo",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Subtle Zoom Icon badge
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(26.dp),
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.65f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Zoom",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = (nameText.take(1)).uppercase(),
                                        color = roleColor,
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = nameText,
                                color = Color(0xFF0F172A),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.3).sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Surface(
                                color = roleColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${departmentText.ifBlank { "General" }} • ${roleText.uppercase()}",
                                    color = roleColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            // Quick Action Buttons (Call / Email)
                            if (phoneText.isNotBlank() || emailText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (phoneText.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFEFF6FF),
                                            modifier = Modifier.clickable {
                                                try {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneText"))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Phone, contentDescription = null, tint = ThemeLogoBlue, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Call", color = ThemeLogoBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    if (emailText.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFF1F5F9),
                                            modifier = Modifier.clickable {
                                                try {
                                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$emailText"))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Outlined.Email, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Email", color = Color(0xFF475569), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Personal Information Card
                item {
                    DetailSectionCard(title = "Personal Information", step = "1") {
                        DetailOutlinedField(
                            value = nameText,
                            onValueChange = { nameText = it },
                            label = "Full Name",
                            icon = Icons.Outlined.Person,
                            isEditMode = isEditMode
                        )

                        DetailOutlinedField(
                            value = emailText,
                            onValueChange = { emailText = it },
                            label = "Email Address",
                            icon = Icons.Outlined.Email,
                            isEditMode = isEditMode
                        )

                        DetailOutlinedField(
                            value = phoneText,
                            onValueChange = { phoneText = it },
                            label = "Phone Number",
                            icon = Icons.Outlined.Phone,
                            isEditMode = isEditMode
                        )
                    }
                }

                // 3. Institutional Details Card
                item {
                    DetailSectionCard(title = "Institutional Details", step = "2") {
                        var deptExpanded by remember { mutableStateOf(false) }
                        val canChangeDept = loggedInRole in listOf("admin", "principal")
                        val deptOptions = listOf("CSE", "ME", "CE", "ECE", "EX", "Civil")

                        Box(modifier = Modifier.fillMaxWidth()) {
                            DetailOutlinedField(
                                value = departmentText,
                                onValueChange = {},
                                label = "Department",
                                icon = Icons.Outlined.AccountBox,
                                isEditMode = false,
                                trailingIcon = if (isEditMode && canChangeDept) Icons.Default.ArrowDropDown else null,
                                onClick = { if (isEditMode && canChangeDept) deptExpanded = true }
                            )
                            DropdownMenu(
                                expanded = deptExpanded,
                                onDismissRequest = { deptExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.88f).background(Color.White)
                            ) {
                                deptOptions.forEach { option ->
                                    DropdownMenuItem(onClick = {
                                        departmentText = option
                                        deptExpanded = false
                                    }) {
                                        Text(option, color = Color(0xFF0F172A), fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        var roleExpanded by remember { mutableStateOf(false) }
                        val roleOptions = if (loggedInRole in listOf("admin", "principal")) {
                            listOf("student", "faculty", "hod", "principal", "admin", "security guard", "reception")
                        } else {
                            listOf("student", "faculty")
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            DetailOutlinedField(
                                value = roleText.replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                label = "Role / Designation",
                                icon = Icons.Outlined.Person,
                                isEditMode = false,
                                trailingIcon = if (isEditMode) Icons.Default.ArrowDropDown else null,
                                onClick = { if (isEditMode) roleExpanded = true }
                            )
                            DropdownMenu(
                                expanded = roleExpanded,
                                onDismissRequest = { roleExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.88f).background(Color.White)
                            ) {
                                roleOptions.forEach { option ->
                                    DropdownMenuItem(onClick = {
                                        roleText = option
                                        roleExpanded = false
                                    }) {
                                        Text(option.replaceFirstChar { it.uppercase() }, color = Color(0xFF0F172A), fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        if (isStudent || batchText.isNotBlank()) {
                            DetailOutlinedField(
                                value = batchText,
                                onValueChange = { batchText = it },
                                label = "Batch",
                                icon = Icons.Outlined.DateRange,
                                isEditMode = isEditMode
                            )
                        }
                    }
                }

                // 4. Student Specific Details Card (if Student)
                if (isStudent || uidText.isNotBlank()) {
                    item {
                        DetailSectionCard(title = "Academic & Guardian Info", step = "3", accentColor = Color(0xFF059669)) {
                            DetailOutlinedField(
                                value = uidText,
                                onValueChange = { uidText = it },
                                label = "Enrollment Number / UID",
                                icon = Icons.Outlined.Info,
                                isEditMode = isEditMode
                            )

                            DetailOutlinedField(
                                value = fatherNameText,
                                onValueChange = { fatherNameText = it },
                                label = "Father's Name",
                                icon = Icons.Outlined.Person,
                                isEditMode = isEditMode
                            )

                            DetailOutlinedField(
                                value = fatherPhoneText,
                                onValueChange = { fatherPhoneText = it },
                                label = "Father's Phone Number",
                                icon = Icons.Outlined.Phone,
                                isEditMode = isEditMode
                            )
                        }
                    }
                }
            }

            // Loading Overlay
            if (isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.4f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            elevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(color = ThemeLogoBlue, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                                Text("Processing...", color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    step: String,
    accentColor: Color = ThemeLogoBlue,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = Color.White,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = step,
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = Color(0xFF0F172A),
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun DetailOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isEditMode: Boolean,
    trailingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = !isEditMode,
        label = {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (value.isNotBlank()) ThemeLogoBlue else Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = trailingIcon?.let {
            {
                Icon(it, contentDescription = null, tint = Color(0xFF64748B))
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0F172A)
        ),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            backgroundColor = if (isEditMode) Color.White else Color(0xFFF8FAFC),
            focusedBorderColor = ThemeLogoBlue,
            unfocusedBorderColor = if (isEditMode) ThemeLogoBlue.copy(alpha = 0.5f) else Color(0xFFE2E8F0),
            focusedLabelColor = ThemeLogoBlue,
            unfocusedLabelColor = Color(0xFF64748B),
            cursorColor = ThemeLogoBlue
        )
    )
}

