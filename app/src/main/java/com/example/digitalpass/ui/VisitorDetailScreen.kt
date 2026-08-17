package com.example.digitalpass.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.digitalpass.LoginUserDataHolder

@Composable
fun VisitorDetailScreen(
    operation: String, // "enter" or "edit"
    visitorData: Map<String, String>?,
    memberList: List<Map<String, String>>,
    isLoading: Boolean,
    capturedBitmap: Bitmap?,
    onBack: () -> Unit,
    onCapturePhoto: () -> Unit,
    onCallPhone: (String) -> Unit,
    onSubmitNewVisitor: (name: String, phone: String, numVisitors: String, reason: String, selectedMember: Map<String, String>) -> Unit,
    onApproveMeet: (remark: String) -> Unit,
    onSecurityExit: () -> Unit
) {
    val role = LoginUserDataHolder.loginUserData?.get("role")?.lowercase() ?: ""
    val isEnterMode = operation == "enter"

    var nameText by remember(visitorData) { mutableStateOf(visitorData?.get("name") ?: "") }
    var phoneText by remember(visitorData) { mutableStateOf(visitorData?.get("phone") ?: "") }
    var numVisitorsText by remember(visitorData) { mutableStateOf(visitorData?.get("numberOfVisitor") ?: "1") }
    var reasonText by remember(visitorData) { mutableStateOf(visitorData?.get("reason") ?: "") }

    var selectedDepartment by remember { mutableStateOf("All Department") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMember by remember(memberList, visitorData) {
        mutableStateOf<Map<String, String>?>(
            if (!isEnterMode && visitorData != null) {
                memberList.firstOrNull { it["email"] == visitorData["meetEmail"] }
                    ?: memberList.firstOrNull()
            } else {
                memberList.firstOrNull { it["role"] == "reception" } ?: memberList.firstOrNull()
            }
        )
    }

    var showRemarkDialog by remember { mutableStateOf(false) }
    var remarkInput by remember { mutableStateOf("") }
    var showAdditionalDetails by remember { mutableStateOf(false) }

    val departments = remember(memberList) {
        val list = mutableListOf("All Department")
        memberList.forEach { member ->
            val dept = member["department"]
            if (!dept.isNullOrBlank() && !list.contains(dept)) {
                list.add(dept)
            }
        }
        list
    }

    val filteredMembers = remember(memberList, selectedDepartment, searchQuery) {
        memberList.filter { member ->
            val matchDept = selectedDepartment == "All Department" || member["department"] == selectedDepartment
            val matchQuery = searchQuery.isBlank() || (member["name"]?.contains(searchQuery, ignoreCase = true) == true)
            matchDept && matchQuery
        }
    }

    Scaffold(
        backgroundColor = Color(0xFFF4F6FA),
        topBar = {
            Surface(
                color = Color.White,
                elevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        elevation = 0.dp
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
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isEnterMode) "New Visitor Entry" else "Visitor Details",
                            color = Color(0xFF0F172A),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isEnterMode) "Reception / Gate Registration" else "Visitor ID: ${visitorData?.get("visitorId") ?: "N/A"}",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                elevation = 16.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    if (isEnterMode) {
                        Button(
                            onClick = {
                                if (selectedMember != null) {
                                    onSubmitNewVisitor(nameText, phoneText, numVisitorsText, reasonText, selectedMember!!)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = ThemeLogoBlue),
                            elevation = ButtonDefaults.elevation(4.dp, 8.dp)
                        ) {
                            Text("Register & Enter Visitor", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        val status = visitorData?.get("status")?.lowercase() ?: ""
                        if (status != "meet") {
                            if (role == "security guard") {
                                Button(
                                    onClick = onSecurityExit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = ThemeLogoBlue),
                                    elevation = ButtonDefaults.elevation(4.dp, 8.dp)
                                ) {
                                    Text("✓ Mark Visitor Exit", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val myEmail = LoginUserDataHolder.loginUserData?.get("email")
                                        val hostEmail = visitorData?.get("meetEmail")
                                        if (myEmail == hostEmail) {
                                            onApproveMeet("")
                                        } else {
                                            showRemarkDialog = true
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = ThemeLogoBlue),
                                    elevation = ButtonDefaults.elevation(4.dp, 8.dp)
                                ) {
                                    Text("✓ Meet & Clear Visitor", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                contentPadding = PaddingValues(top = 18.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. HERO VISITOR IDENTITY & PHOTO CARD
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        backgroundColor = Color.White,
                        elevation = 3.dp,
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Profile Photo with Soft Ring
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF1F5F9))
                                        .border(2.5.dp, ThemeLogoBlue.copy(alpha = 0.25f), CircleShape)
                                        .clickable { if (isEnterMode) onCapturePhoto() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (capturedBitmap != null) {
                                        Image(
                                            bitmap = capturedBitmap.asImageBitmap(),
                                            contentDescription = "Visitor Photo",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (!isEnterMode && !visitorData?.get("img").isNullOrBlank()) {
                                        AsyncImage(
                                            model = LoginUserDataHolder.getURL(visitorData?.get("img") ?: ""),
                                            contentDescription = "Visitor Photo",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Outlined.AccountCircle,
                                                contentDescription = null,
                                                tint = ThemeLogoBlue,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            if (isEnterMode) {
                                                Text("Tap Photo", fontSize = 10.sp, color = ThemeLogoBlue, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEnterMode) {
                                            if (nameText.isNotBlank()) nameText else "New Visitor"
                                        } else {
                                            visitorData?.get("name") ?: "Visitor Name"
                                        },
                                        color = Color(0xFF0F172A),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = if (isEnterMode) "SISTec Campus Entry" else "Campus: ${visitorData?.get("campus") ?: "SISTec"}",
                                        color = Color(0xFF64748B),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (!isEnterMode) {
                                            val statusVal = visitorData?.get("status") ?: "pending"
                                            val statusColor = if (statusVal.lowercase() == "meet") {
                                                Color(0xFF15803D) to Color(0xFFDCFCE7)
                                            } else {
                                                Color(0xFFD97706) to Color(0xFFFEF3C7)
                                            }

                                            Surface(
                                                color = statusColor.second,
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Box(modifier = Modifier.size(6.dp).background(statusColor.first, CircleShape))
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = statusVal.uppercase(),
                                                        color = statusColor.first,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                            }
                                        }

                                        Surface(
                                            color = Color(0xFFEFF6FF),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Text(
                                                text = "👥 Visitor Pass",
                                                color = ThemeLogoBlue,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (!isEnterMode) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(14.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Color(0xFFEDF2F7))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Visitor ID", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(visitorData?.get("visitorId") ?: "N/A", color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Color(0xFFEDF2F7))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Entry Date", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(visitorData?.get("entryDate") ?: "N/A", color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. VISITOR INFORMATION CARD
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        backgroundColor = Color.White,
                        elevation = 3.dp,
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(28.dp),
                                    shape = CircleShape,
                                    color = Color(0xFFEFF6FF)
                                ) {
                                    Icon(
                                        Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = ThemeLogoBlue,
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                                Text("Visitor Information", color = Color(0xFF0F172A), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (isEnterMode) {
                                OutlinedTextField(
                                    value = nameText,
                                    onValueChange = { nameText = it },
                                    label = { Text("Visitor Full Name", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1C1F2E)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = ThemeLogoBlue,
                                        focusedLabelColor = ThemeLogoBlue,
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = phoneText,
                                        onValueChange = { phoneText = it },
                                        label = { Text("Phone Number", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1C1F2E)),
                                        modifier = Modifier.weight(1.3f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = ThemeLogoBlue,
                                            focusedLabelColor = ThemeLogoBlue,
                                            unfocusedBorderColor = Color(0xFFCBD5E1)
                                        )
                                    )

                                    OutlinedTextField(
                                        value = numVisitorsText,
                                        onValueChange = { numVisitorsText = it },
                                        label = { Text("Count", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1C1F2E)),
                                        modifier = Modifier.weight(0.7f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = ThemeLogoBlue,
                                            focusedLabelColor = ThemeLogoBlue,
                                            unfocusedBorderColor = Color(0xFFCBD5E1)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = reasonText,
                                    onValueChange = { reasonText = it },
                                    label = { Text("Reason for Visit", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                                    placeholder = { Text("e.g. Official meeting, parent visit", fontSize = 15.sp) },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1C1F2E), lineHeight = 24.sp),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = ThemeLogoBlue,
                                        focusedLabelColor = ThemeLogoBlue,
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    )
                                )
                            } else {
                                // Shaded Inset Box with Accent Border
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .fillMaxHeight()
                                                .background(ThemeLogoBlue, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                                        )

                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                "REASON FOR VISIT",
                                                color = Color(0xFF64748B),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.8.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = visitorData?.get("reason") ?: "Not specified",
                                                color = Color(0xFF0F172A),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                lineHeight = 22.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Color(0xFFEDF2F7))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("No. of Visitors", color = Color(0xFF64748B), fontSize = 11.sp)
                                            Text(visitorData?.get("numberOfVisitor") ?: "1", color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFEFF6FF),
                                        border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Host Dept", color = ThemeLogoBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text(visitorData?.get("meetDepartment") ?: "General", color = ThemeLogoBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. HOST PERSON TO MEET CARD
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        backgroundColor = Color.White,
                        elevation = 3.dp,
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(28.dp),
                                    shape = CircleShape,
                                    color = Color(0xFFEFF6FF)
                                ) {
                                    Icon(
                                        Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = ThemeLogoBlue,
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                                Text("Person to Meet", color = Color(0xFF0F172A), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (isEnterMode) {
                                var expandedDept by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedDepartment,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Filter Department", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                        trailingIcon = {
                                            IconButton(onClick = { expandedDept = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = ThemeLogoBlue)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().clickable { expandedDept = true },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = ThemeLogoBlue,
                                            unfocusedBorderColor = Color(0xFFCBD5E1)
                                        )
                                    )
                                    DropdownMenu(
                                        expanded = expandedDept,
                                        onDismissRequest = { expandedDept = false }
                                    ) {
                                        departments.forEach { dept ->
                                            DropdownMenuItem(onClick = {
                                                selectedDepartment = dept
                                                expandedDept = false
                                            }) {
                                                Text(dept, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    label = { Text("Search Faculty / Reception", fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = ThemeLogoBlue) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = ThemeLogoBlue,
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    filteredMembers.take(8).forEach { member ->
                                        val isSelected = selectedMember?.get("email") == member["email"]
                                        Surface(
                                            color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, if (isSelected) ThemeLogoBlue else Color(0xFFE2E8F0)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedMember = member }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(member["name"] ?: "Faculty", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                                    Text("${member["department"] ?: ""} • ${member["role"] ?: ""}", fontSize = 12.sp, color = Color(0xFF64748B))
                                                }
                                                if (isSelected) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ThemeLogoBlue, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("HOST FACULTY / RECEPTION", color = ThemeLogoBlue, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = selectedMember?.get("name") ?: visitorData?.get("meetTo") ?: "Assigned Faculty",
                                            color = Color(0xFF1E3A8A),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${visitorData?.get("meetDepartment") ?: selectedMember?.get("department") ?: ""} • ${visitorData?.get("meetEmail") ?: selectedMember?.get("email") ?: ""}",
                                            color = Color(0xFF64748B),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. CONTACT VERIFICATION CARD (in View mode)
                if (!isEnterMode) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            backgroundColor = Color.White,
                            elevation = 3.dp,
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(28.dp),
                                        shape = CircleShape,
                                        color = Color(0xFFEFF6FF)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Phone,
                                            contentDescription = null,
                                            tint = ThemeLogoBlue,
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    }
                                    Text("Contact Verification", color = Color(0xFF0F172A), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Visitor Phone", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(visitorData?.get("phone") ?: "N/A", color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }

                                    val phone = visitorData?.get("phone") ?: ""
                                    if (phone.isNotBlank()) {
                                        Button(
                                            onClick = { onCallPhone(phone) },
                                            shape = RoundedCornerShape(20.dp),
                                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFEFF6FF)),
                                            elevation = ButtonDefaults.elevation(0.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Outlined.Phone, contentDescription = null, tint = ThemeLogoBlue, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Call", color = ThemeLogoBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. ADDITIONAL DETAILS ACCORDION (in View mode)
                if (!isEnterMode) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            backgroundColor = Color.White,
                            elevation = 3.dp,
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showAdditionalDetails = !showAdditionalDetails },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(28.dp),
                                            shape = CircleShape,
                                            color = Color(0xFFF1F5F9)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Menu,
                                                contentDescription = null,
                                                tint = Color(0xFF475569),
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }
                                        Text("Additional Details", color = Color(0xFF0F172A), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(
                                        imageVector = if (showAdditionalDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                }

                                AnimatedVisibility(visible = showAdditionalDetails) {
                                    Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        DetailItem("Visitor ID", visitorData?.get("visitorId") ?: "N/A")
                                        DetailItem("Campus", visitorData?.get("campus") ?: "SISTec")
                                        DetailItem("Entry Date", visitorData?.get("entryDate") ?: "N/A")
                                        if (!visitorData?.get("lastUpdatedBy").isNullOrBlank()) DetailItem("Last Updated By", visitorData?.get("lastUpdatedBy")!!)
                                        if (!visitorData?.get("remark").isNullOrBlank()) DetailItem("Remarks", visitorData?.get("remark")!!)
                                    }
                                }
                            }
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

    // Meeting Clearance Remark Dialog
    if (showRemarkDialog) {
        AlertDialog(
            onDismissRequest = { showRemarkDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Meeting Clearance Remark", color = Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Please enter remarks to clear this visitor entry:", color = Color(0xFF64748B), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = remarkInput,
                        onValueChange = { remarkInput = it },
                        label = { Text("Remark Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = ThemeLogoBlue,
                            focusedLabelColor = ThemeLogoBlue
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (remarkInput.isNotBlank()) {
                            showRemarkDialog = false
                            onApproveMeet(remarkInput)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = ThemeLogoBlue),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("Approve Meet", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemarkDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF64748B), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(value, color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
