package com.example.digitalpass.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import android.content.Intent
import kotlinx.coroutines.*
import androidx.compose.runtime.rememberCoroutineScope
import com.example.digitalpass.R

val ThemeLogoBlue = Color(0xFF0D47A1)

data class NavItem(val id: String, val label: String, val outlinedIcon: ImageVector, val filledIcon: ImageVector)

@Composable
fun MainScaffold(
    role: String,
    onLogout: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onApplyPass: (passType: String, reason: String, destinationCampus: String?) -> Unit = { _, _, _ -> },
    availableCampuses: List<String> = emptyList(),
    onRequestCampusSelection: ((onCampusSelected: (String) -> Unit) -> Unit)? = null,
    onRequestLocation: ((onLocationFetched: (android.location.Location?) -> Unit) -> Unit)? = null,
    contentView: View? = null,
    gatePasses: List<Map<String, String>> = emptyList(),
    visitors: List<Map<String, String>> = emptyList()
) {
    val navItems = remember(role) {
        when (role.lowercase()) {
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
            "faculty", "management member" -> listOf(
                NavItem("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
                NavItem("approvals", "Approvals", Icons.Outlined.CheckCircle, Icons.Filled.CheckCircle),
                NavItem("users", "Users", Icons.Outlined.Person, Icons.Filled.Person),
                NavItem("profile", "Profile", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)
            )
            "reception", "receptionist" -> listOf(
                NavItem("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
                NavItem("entry", "Entry", Icons.Outlined.AddCircle, Icons.Filled.AddCircle),
                NavItem("history", "History", Icons.AutoMirrored.Outlined.List, Icons.AutoMirrored.Filled.List),
                NavItem("profile", "Profile", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)
            )
            "security guard" -> listOf(
                NavItem("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
                NavItem("verify", "Verify", Icons.Outlined.Search, Icons.Filled.Search),
                NavItem("visitors", "Visitors", Icons.Outlined.Person, Icons.Filled.Person),
                NavItem("profile", "Profile", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)
            )
            "student" -> listOf(
                NavItem("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
                NavItem("apply", "Apply Pass", Icons.Outlined.AddCircle, Icons.Filled.AddCircle),
                NavItem("profile", "Profile", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)
            )
            else -> listOf(
                NavItem("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
                NavItem("profile", "Profile", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)
            )
        }
    }

    var selectedItem by remember { mutableStateOf(navItems.first()) }
    var activePassType by remember { mutableStateOf("Regular Pass") }
    var activeApprovalType by remember { mutableStateOf("Gate Pass") }

    var localAppliedPasses by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    val allGatePasses = remember(localAppliedPasses, gatePasses) {
        val list = mutableListOf<Map<String, String>>()
        list.addAll(localAppliedPasses)
        list.addAll(gatePasses)
        list.distinctBy { it["id"] ?: it["gatePassId"] ?: (it["reason"].toString() + it["departureTime"].toString()) }
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {},
        floatingActionButton = {
            if (role.lowercase() != "student" && selectedItem.id == "home") {
                ExtendedFloatingActionButton(
                    text = { Text("Apply Pass", color = Color.White) },
                    icon = { Icon(Icons.Outlined.AddCircle, contentDescription = "Apply Pass", tint = Color.White) },
                    onClick = {
                        activePassType = "Regular Pass"
                        selectedItem = navItems.find { it.id == "apply" } ?: NavItem("apply", "Apply Pass", Icons.Outlined.AddCircle, Icons.Filled.AddCircle)
                    },
                    containerColor = ThemeLogoBlue
                )
            }
        },
        bottomBar = {
            FloatingBottomNavBar(
                items = navItems,
                selectedItem = selectedItem,
                onItemSelected = { item ->
                    when (item.id) {
                        "batches" -> onNavigate("batches")
                        "campus" -> onNavigate("campus")
                        "report" -> onNavigate("report")
                        else -> selectedItem = item
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Crossfade(targetState = selectedItem.id, label = "MainCrossfade") { tabId ->
                when (tabId) {
                    "home" -> {
                        if (contentView != null) {
                            AndroidView(
                                factory = { contentView },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                             HomeContent(
                                role = role,
                                onNavigate = onNavigate,
                                onOpenApplyTab = { passType ->
                                    activePassType = passType
                                    selectedItem = navItems.find { it.id == "apply" } ?: NavItem("apply", "Apply Pass", Icons.Outlined.AddCircle, Icons.Filled.AddCircle)
                                },
                                onOpenApprovalsTab = { approvalType ->
                                    activeApprovalType = approvalType
                                    val approvalsItem = navItems.find { it.id == "approvals" }
                                    if (approvalsItem != null) {
                                        selectedItem = approvalsItem
                                    } else {
                                        onNavigate("approvals")
                                    }
                                },
                                gatePasses = allGatePasses,
                                visitors = visitors
                            )
                        }
                    }
                    "profile" -> ProfileContent(role = role, onNavigate = onNavigate, onLogout = onLogout)
                    "apply" -> StudentApplyTabContent(
                        initialPassType = activePassType,
                        onBack = {
                            selectedItem = navItems.first()
                        },
                        availableCampuses = availableCampuses,
                        onRequestCampusSelection = onRequestCampusSelection,
                        onRequestLocation = onRequestLocation,
                        onSubmit = { passType, reason, destinationCampus ->
                            val timeStr = try {
                                java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                            } catch (e: Exception) {
                                "Just now"
                            }
                            val newPass = mapOf(
                                "id" to System.currentTimeMillis().toString(),
                                "gatePassId" to System.currentTimeMillis().toString(),
                                "name" to (com.example.digitalpass.LoginUserDataHolder.loginUserData?.get("name") ?: "Student"),
                                "email" to (com.example.digitalpass.LoginUserDataHolder.loginUserData?.get("email") ?: ""),
                                "reason" to reason,
                                "status" to "pending",
                                "department" to (com.example.digitalpass.LoginUserDataHolder.loginUserData?.get("department") ?: "B.Tech"),
                                "role" to (com.example.digitalpass.LoginUserDataHolder.loginUserData?.get("role") ?: "Student"),
                                "campus" to (destinationCampus ?: com.example.digitalpass.LoginUserDataHolder.loginUserData?.get("campus") ?: "SISTec Gandhi Nagar"),
                                "destinationCampus" to (destinationCampus ?: ""),
                                "departureTime" to timeStr,
                                "applyDate" to "Today $timeStr"
                            )
                            localAppliedPasses = listOf(newPass) + localAppliedPasses
                            onApplyPass(passType, reason, destinationCampus)
                        }
                    )
                    "approvals" -> ApprovalsTabContent(
                        initialApprovalType = activeApprovalType,
                        gatePasses = allGatePasses,
                        visitors = visitors,
                        onBack = { selectedItem = navItems.first() },
                        onNavigate = onNavigate
                    )
                    "users" -> UsersTabContent(
                        role = role,
                        onBack = { selectedItem = navItems.first() },
                        onNavigate = onNavigate
                    )
                    "batches" -> BatchesTabContent(onNavigate = onNavigate)
                    "campus" -> CampusTabContent(onNavigate = onNavigate)
                    "entry", "visitors" -> VisitorsTabContent(visitors = visitors, onNavigate = onNavigate)
                    "verify" -> VerifyTabContent(onNavigate = onNavigate)
                    "history" -> HistoryTabContent(gatePasses = allGatePasses, visitors = visitors, onNavigate = onNavigate)
                    else -> PlaceholderScreen(selectedItem.label, selectedItem.filledIcon, onOpenFull = { onNavigate(selectedItem.id) })
                }
            }
        }
    }
}

@Composable
fun FloatingBottomNavBar(
    items: List<NavItem>, 
    selectedItem: NavItem, 
    onItemSelected: (NavItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, bottom = 12.dp, top = 4.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.18f)),
            shape = RoundedCornerShape(28.dp),
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = selectedItem.id == item.id
                    val iconColor = if (isSelected) ThemeLogoBlue else Color(0xFF757575)
                    val textColor = if (isSelected) ThemeLogoBlue else Color(0xFF757575)
                    val pillBgColor = if (isSelected) ThemeLogoBlue.copy(alpha = 0.12f) else Color.Transparent
                    val displayIcon = if (isSelected) item.filledIcon else item.outlinedIcon

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onItemSelected(item)
                            }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .background(pillBgColor, RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = displayIcon,
                                contentDescription = item.label,
                                tint = iconColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            color = textColor,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String, icon: ImageVector, onOpenFull: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Box(
                modifier = Modifier.size(80.dp).background(ThemeLogoBlue.copy(alpha = 0.1f), RoundedCornerShape(40.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = ThemeLogoBlue)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, color = Color(0xFF1C1F2E), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material.Button(
                onClick = onOpenFull,
                colors = androidx.compose.material.ButtonDefaults.buttonColors(
                    backgroundColor = ThemeLogoBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Open $title Details", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StudentApplyTabContent(
    initialPassType: String = "Regular Pass",
    availableCampuses: List<String> = emptyList(),
    onBack: () -> Unit = {},
    onRequestCampusSelection: ((onCampusSelected: (String) -> Unit) -> Unit)? = null,
    onRequestLocation: ((onLocationFetched: (android.location.Location?) -> Unit) -> Unit)? = null,
    onSubmit: (passType: String, reason: String, destinationCampus: String?) -> Unit = { _, _, _ -> }
) {
    var selectedPassType by remember(initialPassType) { mutableStateOf(initialPassType) }
    var selectedDestinationCampus by remember { mutableStateOf<String?>(null) }
    var reasonText by remember { mutableStateOf("") }
    var showCustomCampusDialog by remember { mutableStateOf(false) }
    var showSuccessOverlay by remember { mutableStateOf(false) }
    var locationStatus by remember { mutableStateOf("detecting") } // "detecting", "verified", "failed"
    val context = androidx.compose.ui.platform.LocalContext.current

    fun checkLocation() {
        locationStatus = "detecting"
        if (onRequestLocation != null) {
            onRequestLocation.invoke { loc ->
                locationStatus = if (loc != null) "verified" else "failed"
            }
        } else {
            locationStatus = "verified"
        }
    }

    LaunchedEffect(Unit) {
        checkLocation()
    }

    fun openCampusPicker() {
        showCustomCampusDialog = true
    }

    LaunchedEffect(initialPassType) {
        if (initialPassType == "Inter-Campus" && selectedDestinationCampus == null) {
            openCampusPicker()
        }
    }

    val finalCampusList = if (availableCampuses.isNotEmpty()) {
        availableCampuses
    } else {
        listOf("SISTecGN", "SISTecERB", "SISTecR")
    }

    if (showCustomCampusDialog) {
        ProfessionalCampusDialog(
            currentCampus = com.example.digitalpass.LoginUserDataHolder.loginUserData?.get("campus") ?: "",
            availableCampuses = finalCampusList,
            onCampusSelected = { chosen ->
                selectedDestinationCampus = chosen
                showCustomCampusDialog = false
            },
            onDismiss = { showCustomCampusDialog = false }
        )
    }

    val quickReasons = if (selectedPassType == "Inter-Campus") {
        listOf(
            "🔬 Lab / Practical Session",
            "📚 Library / Seminar",
            "🏆 Sports / Inter-College Event",
            "🏛️ Department Official Work"
        )
    } else {
        listOf(
            "🏥 Doctor Appointment",
            "🏠 Going Home / Family",
            "⚡ Urgent Personal Work",
            "🛒 Bank / Market Visit"
        )
    }

    val placeholderText = if (selectedPassType == "Inter-Campus") {
        "Enter purpose of inter-campus visit..."
    } else {
        "Enter reason for departure..."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top Bar Header with proper spacing aligned with all other screens
        Surface(
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = Color(0xFFF1F5F9),
                    shadowElevation = 0.dp
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
                    text = "Apply Pass",
                    color = Color(0xFF0F172A),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Pass Type",
                color = Color(0xFF0F172A),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Capsule Container with Sub-Capsule Selection
            SegmentedToggle(
                options = listOf("Regular Pass", "Inter-Campus"),
                selected = selectedPassType,
                onSelect = {
                    selectedPassType = it
                    if (it == "Inter-Campus") {
                        openCampusPicker()
                    } else {
                        selectedDestinationCampus = null
                    }
                }
            )

            // Destination Campus Card (Shown when Inter-Campus is selected)
            if (selectedPassType == "Inter-Campus") {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Destination Campus",
                    color = Color(0xFF0F172A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (selectedDestinationCampus == null) ThemeGoldenOrange else ThemeLogoBlue
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openCampusPicker() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = "Campus",
                                tint = if (selectedDestinationCampus == null) ThemeGoldenOrange else ThemeLogoBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = selectedDestinationCampus ?: "Tap to choose Destination Campus",
                                color = if (selectedDestinationCampus == null) Color(0xFF64748B) else Color(0xFF0F172A),
                                fontSize = 14.5.sp,
                                fontWeight = if (selectedDestinationCampus == null) FontWeight.Medium else FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (selectedDestinationCampus == null) "Select ▾" else "Change ▾",
                            color = ThemeLogoBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Reason",
                color = Color(0xFF0F172A),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Quick suggestion chips
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickReasons.size) { index ->
                    val chipText = quickReasons[index]
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable {
                            reasonText = chipText.substringAfter(" ")
                        }
                    ) {
                        Text(
                            text = chipText,
                            fontSize = 12.5.sp,
                            color = Color(0xFF334155),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Reason for Pass Input Box
            OutlinedTextField(
                value = reasonText,
                onValueChange = { reasonText = it },
                placeholder = { Text(placeholderText, color = Color(0xFF8E9297), fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThemeLogoBlue,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color(0xFF1C1F2E),
                    unfocusedTextColor = Color(0xFF1C1F2E)
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Location Verification Card (Direct visual indicator like in repo)
            when (locationStatus) {
                "detecting" -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFFBEB),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFFD97706),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Detecting GPS Coordinates...",
                                color = Color(0xFFD97706),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                "verified" -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Verified",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Location Verified • Campus Geofence Active",
                                color = Color(0xFF059669),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                else -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { checkLocation() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Warning,
                                    contentDescription = "Failed",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "GPS Verification Failed",
                                    color = Color(0xFFDC2626),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Retry 🔄",
                                color = Color(0xFFDC2626),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Apply Gate Pass Button
            Button(
                onClick = {
                    if (selectedPassType == "Inter-Campus" && selectedDestinationCampus.isNullOrBlank()) {
                        android.widget.Toast.makeText(context, "Please select destination campus first", android.widget.Toast.LENGTH_SHORT).show()
                        openCampusPicker()
                    } else if (reasonText.trim().isEmpty()) {
                        android.widget.Toast.makeText(context, "Please enter a reason", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        onSubmit(selectedPassType, reasonText.trim(), selectedDestinationCampus)
                        showSuccessOverlay = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeLogoBlue,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 6.dp)
            ) {
                Text(
                    text = "Apply Pass",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        if (showSuccessOverlay) {
            GatePassSuccessCelebrationOverlay(
                passType = selectedPassType,
                destinationCampus = selectedDestinationCampus,
                reason = reasonText,
                onDismiss = {
                    showSuccessOverlay = false
                    reasonText = ""
                    onBack()
                }
            )
        }
    }
}

@Composable
fun GatePassSuccessCelebrationOverlay(
    passType: String,
    destinationCampus: String?,
    reason: String,
    onDismiss: () -> Unit
) {
    var animPhase by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        animPhase = 1 // Fade in background + radial glow
        kotlinx.coroutines.delay(150)
        animPhase = 2 // Bounce circle + expand glow ring
        kotlinx.coroutines.delay(400)
        animPhase = 3 // Pop checkmark + explode confetti + reveal text
        kotlinx.coroutines.delay(2200)
        animPhase = 4 // Fade out
        kotlinx.coroutines.delay(400)
        onDismiss()
    }

    // 1. Overlay Alpha
    val overlayAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = when (animPhase) {
            0 -> 0f
            4 -> 0f
            else -> 0.94f
        },
        animationSpec = androidx.compose.animation.core.tween(350)
    )

    // 2. Success Circle Scale (Spring bounce OvershootInterpolator 2.0f)
    val circleScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animPhase >= 2 && animPhase < 4) 1f else if (animPhase == 4) 0.8f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        )
    )

    // 3. Glow Ring Scale & Alpha
    val ringScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animPhase >= 2) 1.45f else 1.0f,
        animationSpec = androidx.compose.animation.core.tween(800)
    )
    val ringAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animPhase == 2) 0.8f else 0f,
        animationSpec = androidx.compose.animation.core.tween(800)
    )

    // 4. Checkmark Scale (Overshoot bounce 1.8f)
    val checkmarkScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animPhase >= 3 && animPhase < 4) 1f else if (animPhase == 4) 0.8f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioHighBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        )
    )

    // 5. Confetti Progress (0.0 to 1.0)
    val confettiProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animPhase >= 3) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(1200, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    )

    // 6. Text Alpha & Translation
    val textAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animPhase >= 3 && animPhase < 4) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(450)
    )
    val textOffsetY by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (animPhase >= 3) 0.dp else 35.dp,
        animationSpec = androidx.compose.animation.core.tween(450, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    )

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0F1D).copy(alpha = overlayAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            // Large Radial Glow Behind
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            listOf(Color(0xFF00B9F5).copy(alpha = 0.22f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                // Main Animation Center Area
                Box(
                    modifier = Modifier.size(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Expanding Glow Ring
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(ringScale)
                            .border(
                                width = 3.5.dp,
                                color = Color.White.copy(alpha = ringAlpha),
                                shape = CircleShape
                            )
                    )

                    // 6 Confetti Particles bursting radially
                    val confettiAngles = listOf(30.0, 75.0, 120.0, 210.0, 285.0, 330.0)
                    val confettiColors = listOf(
                        Color(0xFFFBBF24), // p1: Gold
                        Color(0xFF60A5FA), // p2: Blue
                        Color(0xFF34D399), // p3: Emerald
                        Color(0xFFF472B6), // p4: Pink
                        Color(0xFFA78BFA), // p5: Purple
                        Color(0xFF38BDF8)  // p6: Cyan
                    )
                    val confettiSizes = listOf(
                        Pair(12.dp, 12.dp),
                        Pair(16.dp, 8.dp),
                        Pair(10.dp, 10.dp),
                        Pair(14.dp, 14.dp),
                        Pair(8.dp, 16.dp),
                        Pair(12.dp, 12.dp)
                    )

                    confettiAngles.indices.forEach { idx ->
                        val angleRad = Math.toRadians(confettiAngles[idx])
                        val maxDist = 130f
                        val currentDist = maxDist * confettiProgress
                        val offsetX = (Math.cos(angleRad) * currentDist).dp
                        val offsetY = (Math.sin(angleRad) * currentDist).dp
                        val particleAlpha = (1f - confettiProgress).coerceIn(0f, 1f)
                        val particleScale = (1f - (0.45f * confettiProgress)).coerceIn(0.2f, 1f)
                        val (w, h) = confettiSizes[idx]

                        Box(
                            modifier = Modifier
                                .offset(x = offsetX, y = offsetY)
                                .size(width = w, height = h)
                                .scale(particleScale)
                                .rotate(360f * confettiProgress)
                                .background(
                                    confettiColors[idx].copy(alpha = particleAlpha),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }

                    // Success White Circle (120dp) with Elevation Shadow
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(circleScale)
                            .shadow(16.dp, CircleShape)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Cyan/Blue Stroke Checkmark (64dp) with Scale animation
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Success",
                            tint = Color(0xFF00B9F5),
                            modifier = Modifier
                                .size(64.dp)
                                .scale(checkmarkScale)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Success Title (26sp, Bold, White)
                Text(
                    text = "Gate Pass Applied!",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .offset(y = textOffsetY)
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Success Subtitle (15sp, #8F9CAE)
                Text(
                    text = "Pending approval from administration",
                    color = Color(0xFF8F9CAE),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .offset(y = textOffsetY)
                        .padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center
                )

                if (destinationCampus != null && destinationCampus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B).copy(alpha = textAlpha),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text(
                            text = "Destination: $destinationCampus",
                            color = Color(0xFF38BDF8),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalCampusDialog(
    currentCampus: String,
    availableCampuses: List<String> = listOf("SISTecGN", "SISTecERB", "SISTecR"),
    onCampusSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val cleanCurrent = currentCampus.lowercase().replace("-", "").replace("sistec", "").trim()
    val filtered = availableCampuses.filter { campusName ->
        if (cleanCurrent.isEmpty()) true
        else {
            val cleanCampus = campusName.lowercase().replace("-", "").replace("sistec", "").trim()
            !cleanCampus.contains(cleanCurrent) && !cleanCurrent.contains(cleanCampus)
        }
    }
    val displayList = if (filtered.isNotEmpty()) filtered else availableCampuses
    var tempSelectedCampus by remember { mutableStateOf(displayList.firstOrNull() ?: "") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(42.dp)
                    .height(4.5.dp)
                    .background(Color(0xFFCBD5E1), RoundedCornerShape(3.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Header with Icon and Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFEFF6FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDBEAFE)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Destination",
                            tint = ThemeLogoBlue,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Select Destination Campus",
                        color = Color(0xFF0F172A),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Choose which SISTec campus you are visiting",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Campus Selection Cards
            displayList.forEach { rawCampus ->
                val isSelected = (rawCampus == tempSelectedCampus)
                val clean = rawCampus.lowercase()
                val (title, tag, subtitle, emoji) = when {
                    clean.contains("gn") || clean.contains("gandhi") -> Quadruple(
                        "SISTec Gandhi Nagar",
                        "GN Main",
                        "Airport Road, Gandhi Nagar, Bhopal",
                        "🏢"
                    )
                    clean.contains("erb") || clean.contains("e") -> Quadruple(
                        "SISTec-E (Engineering)",
                        "ERB Campus",
                        "Engineering & Research, Ratibad",
                        "🏛️"
                    )
                    else -> Quadruple(
                        "SISTec Ratibad",
                        "Ratibad",
                        "Bhadbhada Road, Ratibad, Bhopal",
                        "🏫"
                    )
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) Color(0xFFF0F7FF) else Color(0xFFFAFAFA),
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) ThemeLogoBlue else Color(0xFFE2E8F0)
                    ),
                    shadowElevation = if (isSelected) 3.dp else 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            tempSelectedCampus = rawCampus
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Emoji Badge
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) Color.White else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFBFDBFE) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 20.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    color = Color(0xFF0F172A),
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) ThemeLogoBlue else Color(0xFFE2E8F0)
                                ) {
                                    Text(
                                        text = tag,
                                        color = if (isSelected) Color.White else Color(0xFF475569),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = subtitle,
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        // Radio / Checkbox Indicator
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    if (isSelected) ThemeLogoBlue else Color.Transparent,
                                    CircleShape
                                )
                                .border(
                                    2.dp,
                                    if (isSelected) ThemeLogoBlue else Color(0xFFCBD5E1),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button: Confirm Selection
            Button(
                onClick = {
                    if (tempSelectedCampus.isNotEmpty()) {
                        onCampusSelected(tempSelectedCampus)
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeLogoBlue,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Text(
                    text = "Confirm Destination Campus",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun ApprovalsTabContent(
    initialApprovalType: String = "Gate Pass",
    gatePasses: List<Map<String, String>> = emptyList(),
    visitors: List<Map<String, String>> = emptyList(),
    onBack: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedApprovalType by remember(initialApprovalType) { mutableStateOf(initialApprovalType) }

    val todayDateStr = try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            java.time.LocalDate.now().toString() + " 10:30 AM"
        } else {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        }
    } catch (e: Exception) {
        "2026-08-16 10:30 AM"
    }

    val displayPasses = if (gatePasses.isEmpty()) {
        listOf(
            mapOf(
                "gatePassId" to "GP-1001",
                "name" to "Deepak Kumar",
                "status" to "pending",
                "reason" to "Doctor appointment for regular medical checkup",
                "department" to "B.Tech CSE",
                "role" to "Student",
                "phone" to "9876543210",
                "fatherphone" to "9876543211",
                "fathername" to "Mr. Rajendra Kumar",
                "uid" to "0187CS211045",
                "batch" to "2021-2025",
                "campus" to "SISTec Gandhi Nagar",
                "applyDate" to todayDateStr,
                "departureTime" to "04:30 PM",
                "img" to ""
            ),
            mapOf(
                "gatePassId" to "GP-1002",
                "name" to "Priya Sharma",
                "status" to "approving",
                "reason" to "Going Home for family occasion",
                "department" to "B.Tech ME",
                "role" to "Student",
                "phone" to "9876543212",
                "fatherphone" to "9876543213",
                "fathername" to "Mr. Suresh Sharma",
                "uid" to "0187ME211020",
                "batch" to "2021-2025",
                "campus" to "SISTec Ratibad",
                "applyDate" to todayDateStr,
                "departureTime" to "05:00 PM",
                "img" to ""
            )
        )
    } else gatePasses

    val displayVisitors = if (visitors.isEmpty()) {
        listOf(
            mapOf(
                "visitorId" to "VIS-101",
                "name" to "Dr. Amit Tiwari",
                "phone" to "9876543210",
                "numberOfVisitor" to "1",
                "reason" to "Campus Placement & Internship Discussion",
                "meetDepartment" to "Computer Science",
                "meetEmail" to "hod.cse@sistec.ac.in",
                "meetTo" to "Dr. S. K. Roy (HOD)",
                "status" to "pending",
                "campus" to "SISTec Gandhi Nagar",
                "entryDate" to todayDateStr,
                "img" to ""
            ),
            mapOf(
                "visitorId" to "VIS-102",
                "name" to "Ramesh Verma",
                "phone" to "9876543211",
                "numberOfVisitor" to "2",
                "reason" to "New Admission Query for B.Tech CSE",
                "meetDepartment" to "Admission Cell",
                "meetEmail" to "admission@sistec.ac.in",
                "meetTo" to "Principal Office",
                "status" to "pending",
                "campus" to "SISTec Gandhi Nagar",
                "entryDate" to todayDateStr,
                "img" to ""
            )
        )
    } else visitors

    val pendingPasses = displayPasses.filter { it["status"]?.lowercase() in listOf("pending", "approving") }
    val pendingVisitors = displayVisitors.filter { it["status"]?.lowercase() in listOf("pending", "approving", "entered", "meet") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Clean Top Bar with Back Arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1C1F2E),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Pending Approvals",
                color = Color(0xFF1C1F2E),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp)
        ) {
            // Capsule Segmented Toggle
            item {
                SegmentedToggle(
                    options = listOf("Gate Pass", "Visitor"),
                    selected = selectedApprovalType,
                    onSelect = { selectedApprovalType = it }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (selectedApprovalType == "Gate Pass") {
                if (pendingPasses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = ThemeLightGreen, modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No Pending Gate Passes", color = Color(0xFF1C1F2E), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text("All student pass requests have been cleared.", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(pendingPasses.size) { index ->
                        val pass = pendingPasses[index]
                        val name = pass["name"] ?: "Student"
                        val status = pass["status"] ?: "pending"
                        val img = pass["img"] ?: pass["profilePic"] ?: pass["imageUrl"]

                        UserPhotoCard(
                            name = name,
                            status = status,
                            imageUrl = img,
                            onClick = {
                                val intent = android.content.Intent(context, com.example.digitalpass.GatePassDetail::class.java)
                                intent.putExtra("gatePass", java.util.HashMap(pass))
                                intent.putExtra("operationType", "member")
                                intent.putExtra("listType", "recent")
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            } else {
                if (pendingVisitors.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = ThemeLightGreen, modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No Pending Visitors", color = Color(0xFF1C1F2E), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text("No visitor appointments awaiting approval.", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(pendingVisitors.size) { index ->
                        val visitor = pendingVisitors[index]
                        val name = visitor["name"] ?: "Visitor"
                        val status = visitor["status"] ?: "meet"
                        val img = visitor["img"] ?: visitor["photo"]

                        UserPhotoCard(
                            name = name,
                            status = status,
                            imageUrl = img,
                            onClick = {
                                val intent = android.content.Intent(context, com.example.digitalpass.EnterVisitor::class.java)
                                intent.putExtra("visitor", java.util.HashMap(visitor))
                                intent.putExtra("operation", "edit")
                                intent.putExtra("listType", "recent")
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UsersTabContent(
    role: String,
    onBack: () -> Unit = {},
    onNavigate: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedRole by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // REAL-TIME FLOW from Room Database: Whenever a user is inserted, updated, or deleted,
    // the UI updates instantly and automatically!
    val usersList by produceState<List<Map<String, String>>>(initialValue = emptyList()) {
        try {
            val db = com.example.digitalpass.database.AppDatabase.getDatabase(context)
            db.userDao().getAllUsersFlow().collect { entities ->
                val allUsers = entities.map { it.userData }
                val loggedInRole = com.example.digitalpass.LoginUserDataHolder.loginUserData?.get("role")?.lowercase()?.trim() ?: ""
                val loggedInDept = com.example.digitalpass.LoginUserDataHolder.loginUserData?.get("department")?.trim() ?: ""

                val myEmail = com.example.digitalpass.LoginUserDataHolder.loginUserData?.get("email")?.trim() ?: ""

                val filtered = when (loggedInRole) {
                    "hod" -> {
                        allUsers.filter { user ->
                            val dept = user["department"]?.trim() ?: ""
                            val r = user["role"]?.lowercase()?.trim() ?: ""
                            val email = user["email"]?.trim() ?: ""
                            email != myEmail && dept.equals(loggedInDept, ignoreCase = true) && r !in listOf("admin", "principal", "hod", "security", "security guard", "reception")
                        }
                    }
                    "faculty" -> {
                        allUsers.filter { user ->
                            val dept = user["department"]?.trim() ?: ""
                            val r = user["role"]?.lowercase()?.trim() ?: ""
                            val email = user["email"]?.trim() ?: ""
                            email != myEmail && dept.equals(loggedInDept, ignoreCase = true) && r == "student"
                        }
                    }
                    else -> allUsers.filter { (it["email"]?.trim() ?: "") != myEmail }
                }
                value = filtered
            }
        } catch (_: Exception) { }
    }

    // Trigger initial background network sync when entering the tab if token is present
    LaunchedEffect(Unit) {
        if (com.example.digitalpass.LoginUserDataHolder.token.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                try {
                    val db = com.example.digitalpass.database.AppDatabase.getDatabase(context)
                    val repo = com.example.digitalpass.UserOperationRepository(
                        context,
                        db.campusDao(),
                        db.departmentDao(),
                        db.userDao(),
                        com.example.digitalpass.RetrofitClient.instance
                    )
                    repo.syncUsers(com.example.digitalpass.LoginUserDataHolder.token)
                } catch (_: Exception) { }
            }
        }
    }

    UserManagementScreen(
        userList = usersList,
        selectedRoleFilter = selectedRole,
        searchQuery = searchQuery,
        selectedUserEmails = emptySet(),
        isLoading = isLoading,
        onBack = onBack,
        onSync = {
            // Trigger actual network sync with backend server
            scope.launch {
                isLoading = true
                withContext(Dispatchers.IO) {
                    try {
                        val db = com.example.digitalpass.database.AppDatabase.getDatabase(context)
                        val repo = com.example.digitalpass.UserOperationRepository(
                            context,
                            db.campusDao(),
                            db.departmentDao(),
                            db.userDao(),
                            com.example.digitalpass.RetrofitClient.instance
                        )
                        repo.syncUsers(com.example.digitalpass.LoginUserDataHolder.token)
                    } catch (_: Exception) { }
                }
                isLoading = false
            }
        },
        onRoleSelect = { selectedRole = it },
        onSearchChange = { searchQuery = it },
        onUserClick = { user ->
            val intent = Intent(context, com.example.digitalpass.UserManagementViewUser::class.java).apply {
                putExtra("user", HashMap(user))
            }
            context.startActivity(intent)
        },
        onUserLongClick = { },
        onDeleteSelected = { },
        onAddUser = {
            val intent = Intent(context, com.example.digitalpass.AddUser::class.java)
            context.startActivity(intent)
        },
        isEmbedded = true
    )
}

@Composable
fun BatchesTabContent(
    onNavigate: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        item {
            Text("Batches & Classes", color = Color(0xFF1C1F2E), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Department and class semester groups", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(20.dp))
            androidx.compose.material.Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clickable { onNavigate("batches") },
                shape = RoundedCornerShape(16.dp),
                backgroundColor = ThemeLogoBlue,
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.DateRange, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Manage Batches & Semesters", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CampusTabContent(
    onNavigate: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        item {
            Text("Campus Management", color = Color(0xFF1C1F2E), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Campus branches and entry/exit gates configuration", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(20.dp))
            androidx.compose.material.Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clickable { onNavigate("campus") },
                shape = RoundedCornerShape(16.dp),
                backgroundColor = ThemeLogoBlue,
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Configure Campus & Gates", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun VisitorsTabContent(
    visitors: List<Map<String, String>>,
    onNavigate: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        item {
            Text("Visitor Management", color = Color(0xFF1C1F2E), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBox(Modifier.weight(1f), visitors.size.toString(), "Total Visitors", ThemeDarkBlue)
                val exited = visitors.count { it["status"]?.lowercase() == "exited" }
                StatBox(Modifier.weight(1f), exited.toString(), "Exited", ThemeLightGreen)
            }
        }
        item {
            androidx.compose.material.Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clickable { onNavigate("entry") },
                shape = RoundedCornerShape(16.dp),
                backgroundColor = ThemeLightGreen,
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Register New Visitor Entry", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Recent Visitors", color = Color(0xFF1C1F2E), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            if (visitors.isEmpty()) {
                Text("No visitors recorded today.", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                visitors.take(6).forEach { visitor ->
                    val name = visitor["name"] ?: "Visitor"
                    val meetTo = visitor["meetTo"] ?: "Campus"
                    ActionListItem("$name (Host: $meetTo)", Icons.Outlined.Person, ThemeLogoBlue) {
                        onNavigate("history")
                    }
                }
            }
        }
    }
}

@Composable
fun VerifyTabContent(
    onNavigate: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(100.dp).background(ThemeLogoBlue.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(48.dp), tint = ThemeLogoBlue)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Gate Verification", color = Color(0xFF1C1F2E), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Search & verify approved student and visitor passes", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(28.dp))
            androidx.compose.material.Button(
                onClick = { onNavigate("verify") },
                colors = androidx.compose.material.ButtonDefaults.buttonColors(
                    backgroundColor = ThemeLogoBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search & Verify Passes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun HistoryTabContent(
    gatePasses: List<Map<String, String>>,
    visitors: List<Map<String, String>>,
    onNavigate: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        item {
            Text("Pass & Visitor History", color = Color(0xFF1C1F2E), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Complete logs of entries, exits, and gate passes", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(20.dp))
            androidx.compose.material.Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clickable { onNavigate("history") },
                shape = RoundedCornerShape(16.dp),
                backgroundColor = ThemeLogoBlue,
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.List, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("View Full History Database", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    role: String,
    onNavigate: (String) -> Unit,
    onOpenApplyTab: (passType: String) -> Unit = {},
    onOpenApprovalsTab: (approvalType: String) -> Unit = {},
    gatePasses: List<Map<String, String>>,
    visitors: List<Map<String, String>>
) {
    val collegeName = com.example.digitalpass.LoginUserDataHolder.loginUserData?.get("campus") ?: "SISTec"
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF021736))
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(52.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sistec_official_logo),
                        contentDescription = "SISTec Official Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(1.dp)
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "SISTec Digital Pass",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = collegeName,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            when (role.lowercase()) {
                "student" -> StudentDashboardBody(
                    onApplyRegularPass = { onOpenApplyTab("Regular Pass") },
                    onApplyInterCampusPass = { onOpenApplyTab("Inter-Campus") },
                    gatePasses = gatePasses
                )
                "security guard" -> SecurityDashboardBody(
                    onScanGatePass = { onNavigate("verify") },
                    onNewVisitorEntry = { onNavigate("visitors") },
                    gatePasses = gatePasses,
                    visitors = visitors
                )
                "reception", "receptionist" -> ReceptionDashboardBody(
                    onNewVisitorEntry = { onNavigate("entry") },
                    visitors = visitors
                )
                else -> ManagementDashboardBody(
                    role = role,
                    onNavigate = { route ->
                        if (route == "apply") {
                            onOpenApplyTab("Regular Pass")
                        } else {
                            onNavigate(route)
                        }
                    },
                    onOpenApprovalsTab = onOpenApprovalsTab,
                    gatePasses = gatePasses,
                    visitors = visitors
                )
            }
        }
    }
}

@Composable
fun ProfileContent(
    role: String,
    onNavigate: (String) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val userDetails = com.example.digitalpass.LoginUserDataHolder.loginUserData
    val name = userDetails?.get("name") ?: "Demo User"
    val email = userDetails?.get("email") ?: "${role.lowercase()}@sistec.ac.in"
    val displayRole = role.replaceFirstChar { it.uppercase() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 100.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "My Profile",
                    color = Color(0xFF1C1F2E),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }
        }

        // Profile Details Card
        item {
            androidx.compose.material.Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = Color.White,
                elevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(ThemeLogoBlue.copy(alpha = 0.1f), CircleShape)
                            .border(2.dp, ThemeLogoBlue.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(displayRole.take(1).uppercase(), color = ThemeLogoBlue, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(name, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = Color(0xFF1C1F2E))
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(email, color = Color(0xFF64748B), fontSize = 13.5.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        androidx.compose.material.Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ThemeLogoBlue.copy(alpha = 0.1f)
                        ) {
                            Text(
                                displayRole,
                                color = ThemeLogoBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Activity & History Section
        item {
            Text("Activity & Records", color = Color(0xFF1C1F2E), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            ActionListItem("Gate Pass & Visitor History", Icons.AutoMirrored.Outlined.List, ThemeLogoBlue) {
                onNavigate("history")
            }
            ActionListItem("Download Activity Reports", Icons.Outlined.Info, ThemeLogoBlue) {
                onNavigate("report")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Account Actions
        item {
            Text("Account", color = Color(0xFF1C1F2E), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            androidx.compose.material.Button(
                onClick = onLogout,
                colors = androidx.compose.material.ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFFFEBEE),
                    contentColor = Color(0xFFD32F2F)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                elevation = androidx.compose.material.ButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = "Logout", tint = Color(0xFFD32F2F))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
