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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.digitalpass.LoginUserDataHolder
import com.example.digitalpass.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun UserManagementScreen(
    userList: List<Map<String, String>>,
    selectedRoleFilter: String,
    searchQuery: String,
    selectedUserEmails: Set<String>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSync: () -> Unit,
    onRoleSelect: (role: String) -> Unit,
    onSearchChange: (query: String) -> Unit,
    onUserClick: (user: Map<String, String>) -> Unit,
    onUserLongClick: (email: String) -> Unit,
    onDeleteSelected: () -> Unit,
    onAddUser: () -> Unit = {},
    isEmbedded: Boolean = false
) {
    val context = LocalContext.current
    var selectedDepartmentFilter by remember { mutableStateOf("All") }
    var showExcelUploadDialog by remember { mutableStateOf(false) }

    // Fallback sample users if database is empty for demo/testing
    val loggedInRole = LoginUserDataHolder.loginUserData?.get("role")?.lowercase()?.trim() ?: ""
    val loggedInDept = LoginUserDataHolder.loginUserData?.get("department")?.trim() ?: ""
    val myEmail = LoginUserDataHolder.loginUserData?.get("email")?.trim() ?: ""
    val isHOD = loggedInRole == "hod"

    val effectiveUsers = remember(userList, isHOD, loggedInDept, myEmail) {
        val base = if (userList.isNotEmpty()) userList else {
            listOf(
                mapOf(
                    "name" to "Yogesh Saini",
                    "email" to "yogesh@sistec.ac.in",
                    "phone" to "9826012345",
                    "role" to "student",
                    "department" to "CSE",
                    "batch" to "2021-2025",
                    "uid" to "0187CS211045",
                    "campus" to "SISTec-R"
                ),
                mapOf(
                    "name" to "Prof. Anjali Verma",
                    "email" to "anjali.verma@sistec.ac.in",
                    "phone" to "9826098765",
                    "role" to "faculty",
                    "department" to "CSE",
                    "campus" to "SISTec-R"
                )
            )
        }

        when {
            isHOD -> {
                base.filter { user ->
                    val dept = user["department"]?.trim() ?: ""
                    val role = user["role"]?.lowercase()?.trim() ?: ""
                    val email = user["email"]?.trim() ?: ""
                    email != myEmail && dept.equals(loggedInDept, ignoreCase = true) && role !in listOf("admin", "principal", "hod", "security", "security guard", "reception")
                }
            }
            loggedInRole == "faculty" -> {
                base.filter { user ->
                    val dept = user["department"]?.trim() ?: ""
                    val role = user["role"]?.lowercase()?.trim() ?: ""
                    val email = user["email"]?.trim() ?: ""
                    email != myEmail && dept.equals(loggedInDept, ignoreCase = true) && role == "student"
                }
            }
            else -> base.filter { (it["email"]?.trim() ?: "") != myEmail }
        }
    }

    // Role Counts for Metric Analytics
    val totalCount = effectiveUsers.size
    val studentCount = remember(effectiveUsers) { effectiveUsers.count { it["role"]?.lowercase()?.trim() == "student" } }
    val facultyCount = remember(effectiveUsers) {
        effectiveUsers.count {
            it["role"]?.lowercase()?.trim() in listOf("principal", "hod", "faculty", "admin", "management member", "teacher", "tg")
        }
    }
    val securityCount = remember(effectiveUsers) { effectiveUsers.count { it["role"]?.lowercase()?.trim() == "security guard" } }
    val receptionCount = remember(effectiveUsers) { effectiveUsers.count { it["role"]?.lowercase()?.trim() == "reception" } }

    // Department List for Filters
    val departments = remember(effectiveUsers) {
        val list = effectiveUsers.mapNotNull { it["department"]?.takeIf { d -> d.isNotBlank() } }.distinct().sorted()
        listOf("All") + list
    }

    // Filter Logic
    val filteredUsers = remember(effectiveUsers, selectedRoleFilter, selectedDepartmentFilter, searchQuery) {
        effectiveUsers.filter { user ->
            val role = user["role"]?.lowercase()?.trim() ?: ""
            val roleMatch = when (selectedRoleFilter) {
                "All" -> true
                "Student" -> role == "student"
                "Management", "Faculty" -> role in listOf("principal", "hod", "faculty", "admin", "management member", "teacher", "tg")
                "Security" -> role == "security guard"
                "Reception" -> role == "reception"
                else -> true
            }

            val deptMatch = selectedDepartmentFilter == "All" ||
                    (user["department"]?.trim()?.equals(selectedDepartmentFilter.trim(), ignoreCase = true) == true)

            val nameMatch = searchQuery.isBlank() ||
                    (user["name"]?.contains(searchQuery, ignoreCase = true) == true) ||
                    (user["email"]?.contains(searchQuery, ignoreCase = true) == true) ||
                    (user["department"]?.contains(searchQuery, ignoreCase = true) == true) ||
                    (user["uid"]?.contains(searchQuery, ignoreCase = true) == true)

            roleMatch && deptMatch && nameMatch
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
                        .then(if (!isEmbedded) Modifier.statusBarsPadding() else Modifier)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(38.dp),
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
                        Text(
                            text = "User Management",
                            color = Color(0xFF0F172A),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val userRole = LoginUserDataHolder.loginUserData?.get("role")?.lowercase()?.trim() ?: ""
                        val canUploadExcel = userRole in listOf("admin", "principal", "hod", "faculty")

                        if (canUploadExcel) {
                            // Professional Excel Badge Button
                            Surface(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showExcelUploadDialog = true },
                                color = Color(0xFFE8F5E9),
                                border = BorderStroke(1.dp, Color(0xFFC8E6C9))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFF107C41),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "X",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Import",
                                        color = Color(0xFF1B5E20),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Sync Button
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            elevation = 0.dp
                        ) {
                            IconButton(onClick = onSync) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync",
                                    tint = ThemeLogoBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            val bottomBarRole = LoginUserDataHolder.loginUserData?.get("role")?.lowercase()?.trim() ?: ""
            val canBatchDelete = bottomBarRole in listOf("admin", "principal")

            if (selectedUserEmails.isNotEmpty() && canBatchDelete) {
                Surface(
                    color = Color.White,
                    elevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${selectedUserEmails.size} Users Selected",
                                color = Color(0xFF0F172A),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Batch deletion action",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = onDeleteSelected,
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.elevation(2.dp, 4.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Users", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (!isEmbedded) {
                val navRole = bottomBarRole
                val navItems = remember(navRole) {
                    when (navRole) {
                        "admin", "principal" -> listOf(
                            NavItem("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
                            NavItem("approvals", "Approvals", Icons.Outlined.CheckCircle, Icons.Filled.CheckCircle),
                            NavItem("users", "Users", Icons.Outlined.Person, Icons.Filled.Person),
                            NavItem("campus", "Campus", Icons.Outlined.LocationOn, Icons.Filled.LocationOn),
                            NavItem("profile", "Profile", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)
                        )
                        "hod" -> listOf(
                            NavItem("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
                            NavItem("approvals", "Approvals", Icons.Outlined.CheckCircle, Icons.Filled.CheckCircle),
                            NavItem("users", "Users", Icons.Outlined.Person, Icons.Filled.Person),
                            NavItem("batches", "Batches", Icons.Outlined.DateRange, Icons.Filled.DateRange),
                            NavItem("profile", "Profile", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)
                        )
                        else -> listOf(
                            NavItem("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
                            NavItem("approvals", "Approvals", Icons.Outlined.CheckCircle, Icons.Filled.CheckCircle),
                            NavItem("users", "Users", Icons.Outlined.Person, Icons.Filled.Person),
                            NavItem("profile", "Profile", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)
                        )
                    }
                }

                val selectedNavItem = remember(navItems) { navItems.find { it.id == "users" } ?: navItems.first() }

                Box(modifier = Modifier.navigationBarsPadding()) {
                    FloatingBottomNavBar(
                        items = navItems,
                        selectedItem = selectedNavItem,
                        onItemSelected = { item ->
                            if (item.id != "users") {
                                onBack()
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            val userRole = LoginUserDataHolder.loginUserData?.get("role")?.lowercase()?.trim() ?: ""
            val canAddUser = userRole in listOf("admin", "principal", "hod", "faculty")
            if (selectedUserEmails.isEmpty() && canAddUser) {
                ExtendedFloatingActionButton(
                    text = { Text("Add User", color = Color.White, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add User", tint = Color.White) },
                    onClick = onAddUser,
                    backgroundColor = ThemeLogoBlue,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
            ) {
                // 1. Analytics Role Metric Cards (Interactive, Scoped strictly by RBAC)
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    when (loggedInRole) {
                        "faculty" -> {
                            // Full-width balanced 2 cards grid
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatMetricCard(
                                    title = "All Students",
                                    count = "$studentCount",
                                    iconEmoji = "👥",
                                    isSelected = selectedRoleFilter == "All",
                                    activeColor = ThemeLogoBlue,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onRoleSelect("All") }
                                )
                                StatMetricCard(
                                    title = "$loggedInDept Dept",
                                    count = "$totalCount",
                                    iconEmoji = "🎓",
                                    isSelected = selectedRoleFilter == "Student",
                                    activeColor = Color(0xFF2563EB),
                                    modifier = Modifier.weight(1f),
                                    onClick = { onRoleSelect("Student") }
                                )
                            }
                        }
                        "hod" -> {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    StatMetricCard(
                                        title = "$loggedInDept Total",
                                        count = "$totalCount",
                                        iconEmoji = "👥",
                                        isSelected = selectedRoleFilter == "All",
                                        activeColor = ThemeLogoBlue,
                                        onClick = { onRoleSelect("All") }
                                    )
                                }
                                item {
                                    StatMetricCard(
                                        title = "Students",
                                        count = "$studentCount",
                                        iconEmoji = "🎓",
                                        isSelected = selectedRoleFilter == "Student",
                                        activeColor = Color(0xFF2563EB),
                                        onClick = { onRoleSelect("Student") }
                                    )
                                }
                                item {
                                    StatMetricCard(
                                        title = "Faculty",
                                        count = "$facultyCount",
                                        iconEmoji = "👨‍🏫",
                                        isSelected = selectedRoleFilter == "Management" || selectedRoleFilter == "Faculty",
                                        activeColor = Color(0xFF059669),
                                        onClick = { onRoleSelect("Management") }
                                    )
                                }
                            }
                        }
                        else -> {
                            // Admin / Principal
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    StatMetricCard(
                                        title = "All Users",
                                        count = "$totalCount",
                                        iconEmoji = "👥",
                                        isSelected = selectedRoleFilter == "All",
                                        activeColor = ThemeLogoBlue,
                                        onClick = { onRoleSelect("All") }
                                    )
                                }
                                item {
                                    StatMetricCard(
                                        title = "Students",
                                        count = "$studentCount",
                                        iconEmoji = "🎓",
                                        isSelected = selectedRoleFilter == "Student",
                                        activeColor = Color(0xFF2563EB),
                                        onClick = { onRoleSelect("Student") }
                                    )
                                }
                                item {
                                    StatMetricCard(
                                        title = "Faculty",
                                        count = "$facultyCount",
                                        iconEmoji = "👨‍🏫",
                                        isSelected = selectedRoleFilter == "Management" || selectedRoleFilter == "Faculty",
                                        activeColor = Color(0xFF059669),
                                        onClick = { onRoleSelect("Management") }
                                    )
                                }
                                item {
                                    StatMetricCard(
                                        title = "Security",
                                        count = "$securityCount",
                                        iconEmoji = "🛡️",
                                        isSelected = selectedRoleFilter == "Security",
                                        activeColor = Color(0xFFD97706),
                                        onClick = { onRoleSelect("Security") }
                                    )
                                }
                                item {
                                    StatMetricCard(
                                        title = "Reception",
                                        count = "$receptionCount",
                                        iconEmoji = "🏢",
                                        isSelected = selectedRoleFilter == "Reception",
                                        activeColor = Color(0xFF7C3AED),
                                        onClick = { onRoleSelect("Reception") }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Modern Elevated Search Bar (Single Row)
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = {
                            Text(
                                text = "Search users by name, UID...",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = ThemeLogoBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        maxLines = 1,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0F172A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            backgroundColor = Color.White,
                            focusedBorderColor = ThemeLogoBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            cursorColor = ThemeLogoBlue
                        )
                    )
                }

                // 3. Department Filter Chips (Only for Admin / Principal)
                val isAdminRole = loggedInRole in listOf("admin", "principal")
                if (isAdminRole) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(departments) { dept ->
                                val isSelected = selectedDepartmentFilter == dept
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) Color(0xFF1E293B) else Color.White,
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                                    elevation = if (isSelected) 2.dp else 0.dp,
                                    modifier = Modifier.clickable { selectedDepartmentFilter = dept }
                                ) {
                                    Text(
                                        text = if (dept == "All") "All Branches" else dept,
                                        color = if (isSelected) Color.White else Color(0xFF475569),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. User Cards List
                if (filteredUsers.isEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountCircle,
                                    contentDescription = null,
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No users found",
                                    color = Color(0xFF64748B),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Try adjusting your search or filters",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    items(filteredUsers, key = { it["email"] ?: "" }) { user ->
                        val email = user["email"] ?: ""
                        val isSelected = selectedUserEmails.contains(email)

                        UserEnterpriseCard(
                            user = user,
                            isSelected = isSelected,
                            onClick = { onUserClick(user) },
                            onLongClick = { onUserLongClick(email) },
                            onCallClick = { phone ->
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // ignore
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            // Loading Overlay
            if (isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.35f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ThemeLogoBlue)
                    }
                }
            }

            // Bulk Excel Upload Dialog Modal
            if (showExcelUploadDialog) {
                BulkExcelUploadDialog(
                    onDismiss = { showExcelUploadDialog = false },
                    onUploadSuccess = {
                        showExcelUploadDialog = false
                        onSync()
                    }
                )
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    count: String,
    iconEmoji: String,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier.width(110.dp),
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) activeColor else Color.White,
        elevation = if (isSelected) 6.dp else 2.dp,
        border = BorderStroke(1.dp, if (isSelected) activeColor else Color(0xFFEDF2F7))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = iconEmoji, fontSize = 20.sp)
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = count,
                color = if (isSelected) Color.White else Color(0xFF0F172A),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = title,
                color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun UserEnterpriseCard(
    user: Map<String, String>,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCallClick: (phone: String) -> Unit
) {
    val imgUrl = user["img"] ?: user["profilePic"] ?: user["imageUrl"]
    val role = (user["role"] ?: "User").lowercase()
    val isStudent = role == "student"

    val roleColor = when {
        isStudent -> Color(0xFF2563EB)
        role in listOf("hod", "principal", "admin") -> Color(0xFF7C3AED)
        role == "faculty" || role == "teacher" || role == "tg" -> Color(0xFF059669)
        role == "security guard" -> Color(0xFFD97706)
        else -> Color(0xFF475569)
    }

    val roleBgColor = roleColor.copy(alpha = 0.12f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
        elevation = if (isSelected) 4.dp else 2.dp,
        border = BorderStroke(1.dp, if (isSelected) ThemeLogoBlue else Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Profile Avatar with Ring
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .border(2.dp, roleColor.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!imgUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = LoginUserDataHolder.getURL(imgUrl),
                            contentDescription = "User Photo",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = (user["name"]?.take(1) ?: "U").uppercase(),
                            color = roleColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = user["name"] ?: "User Name",
                            color = Color(0xFF0F172A),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Role Pill Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = roleBgColor
                        ) {
                            Text(
                                text = (user["role"] ?: "User").uppercase(),
                                color = roleColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = user["email"] ?: "",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user["department"] ?: "General",
                            color = Color(0xFF334155),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (!user["uid"].isNullOrBlank()) {
                            Text(text = " • ", color = Color(0xFF94A3B8))
                            Text(
                                text = user["uid"] ?: "",
                                color = Color(0xFF2563EB),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val rawBatch = user["batch"] ?: ""
                        val isValidBatch = isStudent && rawBatch.isNotBlank() && !rawBatch.contains("SISTec", ignoreCase = true) && !rawBatch.contains("-", ignoreCase = true)
                        if (isValidBatch) {
                            Text(text = " • ", color = Color(0xFF94A3B8))
                            Text(
                                text = rawBatch,
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Checkbox(
                        checked = true,
                        onCheckedChange = { onLongClick() },
                        colors = CheckboxDefaults.colors(checkedColor = ThemeLogoBlue)
                    )
                }
            }

            // Quick Action Row (Direct Call)
            val phone = user["phone"] ?: user["fatherphone"]
            if (!phone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📱 $phone",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.clickable { onCallClick(phone) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call",
                                tint = ThemeLogoBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Call",
                                color = ThemeLogoBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BulkExcelUploadDialog(
    onDismiss: () -> Unit,
    onUploadSuccess: () -> Unit
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            var name: String? = null
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (_: Exception) {}
            selectedFileName = name ?: "student_records.xlsx"
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            elevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE8F5E9),
                            border = BorderStroke(1.dp, Color(0xFFC8E6C9))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(
                                    color = Color(0xFF107C41),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "X",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Bulk Import Students",
                            color = Color(0xFF0F172A),
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isUploading,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Guidance Info Box
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Excel Sheet Format Guidelines:",
                            color = Color(0xFF0F172A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Columns required: name, email, phone, uid, fathername, fatherphone, batch, department\n• File types supported: .xlsx, .xls, .csv\n• Maximum file size limit: 10 MB",
                            color = Color(0xFF475569),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // File Chooser Box
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .clickable(enabled = !isUploading) {
                            filePickerLauncher.launch("*/*")
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selectedUri != null) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (selectedUri != null) Color(0xFF10B981) else Color(0xFFCBD5E1)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (selectedUri != null) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = selectedFileName ?: "File Selected",
                                color = Color(0xFF065F46),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap to choose different file",
                                color = Color(0xFF059669),
                                fontSize = 11.5.sp
                            )
                        } else {
                            Surface(
                                color = Color(0xFF107C41),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "XLS",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Click to Select Excel Spreadsheet",
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Supports .xlsx, .xls, .csv files",
                                color = Color(0xFF64748B),
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Upload Button
                Button(
                    onClick = {
                        val uri = selectedUri ?: return@Button
                        isUploading = true

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val contentResolver = context.contentResolver
                                val tempFile = java.io.File(context.cacheDir, selectedFileName ?: "upload_users.xlsx")
                                contentResolver.openInputStream(uri)?.use { input ->
                                    java.io.FileOutputStream(tempFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }

                                val reqFile = tempFile.asRequestBody("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".toMediaTypeOrNull())
                                val filePart = MultipartBody.Part.createFormData("file", tempFile.name, reqFile)
                                val tokenPart = LoginUserDataHolder.token.toRequestBody("text/plain".toMediaTypeOrNull())

                                val call = RetrofitClient.instance.uploadExcelUsers(filePart, tokenPart)
                                val response = call.execute()

                                withContext(Dispatchers.Main) {
                                    isUploading = false
                                    if (response.isSuccessful) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Excel uploaded and students imported successfully!",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                        onUploadSuccess()
                                    } else {
                                        val errorMsg = LoginUserDataHolder.getErrorMessage(response)
                                        android.widget.Toast.makeText(
                                            context,
                                            errorMsg.ifBlank { "Failed to import excel sheet" },
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isUploading = false
                                    android.widget.Toast.makeText(
                                        context,
                                        "Error uploading: ${e.localizedMessage}",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    enabled = selectedUri != null && !isUploading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF059669),
                        disabledBackgroundColor = Color(0xFFCBD5E1)
                    )
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Importing Students...", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload & Import Records", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
