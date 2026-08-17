package com.example.digitalpass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StudentDashboardBody(
    onApplyRegularPass: () -> Unit = {},
    onApplyInterCampusPass: () -> Unit = {},
    onViewAllRecentPasses: () -> Unit = {},
    gatePasses: List<Map<String, String>> = emptyList()
) {
    val activePass = gatePasses.firstOrNull { it["status"]?.lowercase() in listOf("pending", "approved") }
    val context = androidx.compose.ui.platform.LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp)
    ) {
        item {
            Text("Active Pass Ticket", color = Color(0xFF1C1F2E), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            if (activePass != null) {
                ActivePassTicketWidget(activePass, onClick = {
                    val intent = android.content.Intent(context, com.example.digitalpass.GatePassDetail::class.java)
                    intent.putExtra("gatePass", java.util.HashMap(activePass))
                    intent.putExtra("operationType", "self")
                    intent.putExtra("listType", "recent")
                    context.startActivity(intent)
                })
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📋", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("No Active Pass", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                            Text("Apply for a pass using quick actions below", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
        item {
            Text("Quick Actions", color = Color(0xFF1C1F2E), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Regular Pass",
                    icon = Icons.Outlined.AddCircle
                ) {
                    onApplyRegularPass()
                }
                SmallActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Inter-Campus",
                    icon = Icons.Outlined.LocationOn
                ) {
                    onApplyInterCampusPass()
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Passes", color = Color(0xFF1C1F2E), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "View All →",
                    color = ThemeLogoBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onViewAllRecentPasses() }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            if (gatePasses.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No pass history found.",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                gatePasses.take(4).forEach { pass ->
                    StudentRecentPassCard(pass = pass, onClick = {
                        val intent = android.content.Intent(context, com.example.digitalpass.GatePassDetail::class.java)
                        intent.putExtra("gatePass", java.util.HashMap(pass))
                        intent.putExtra("operationType", "self")
                        intent.putExtra("listType", "recent")
                        context.startActivity(intent)
                    })
                }
            }
        }
    }
}

@Composable
fun StudentRecentPassCard(pass: Map<String, String>, onClick: () -> Unit = {}) {
    val status = pass["status"]?.lowercase() ?: "pending"
    val reason = pass["reason"]?.takeIf { it.isNotEmpty() } ?: "Gate Pass"
    val isInterCampus = pass["destinationCampus"]?.isNotEmpty() == true || pass["passType"]?.contains("inter", ignoreCase = true) == true
    val campus = pass["destinationCampus"]?.takeIf { it.isNotEmpty() } ?: pass["campus"] ?: "Local Campus"
    val time = pass["departureTime"] ?: pass["applyDate"] ?: "Recently"

    val (statusColor, statusBg, statusLabel) = when (status) {
        "approved" -> Triple(Color(0xFF059669), Color(0xFFECFDF5), "Approved")
        "rejected" -> Triple(Color(0xFFDC2626), Color(0xFFFEF2F2), "Rejected")
        else -> Triple(Color(0xFFD97706), Color(0xFFFFFBEB), "Pending")
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isInterCampus) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isInterCampus) "🏛️" else "🎫",
                            fontSize = 20.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = reason,
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isInterCampus) "📍 $campus • $time" else "🕒 $time",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = statusBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SecurityDashboardBody(
    onScanGatePass: () -> Unit = {},
    onNewVisitorEntry: () -> Unit = {},
    gatePasses: List<Map<String, String>> = emptyList(),
    visitors: List<Map<String, String>> = emptyList()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedTab by remember { mutableStateOf("Gate Pass") }
    var searchQuery by remember { mutableStateOf("") }

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
                "status" to "approved",
                "reason" to "Doctor appointment for regular checkup",
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
                "status" to "approved",
                "reason" to "Going Home for family occasion",
                "department" to "B.Tech ME",
                "role" to "Student",
                "phone" to "9876543212",
                "fatherphone" to "9876543213",
                "fathername" to "Mr. Suresh Sharma",
                "uid" to "0187ME211020",
                "batch" to "2021-2025",
                "campus" to "SISTec Gandhi Nagar",
                "applyDate" to todayDateStr,
                "departureTime" to "05:00 PM",
                "img" to ""
            ),
            mapOf(
                "gatePassId" to "GP-1003",
                "name" to "Rahul Verma",
                "status" to "exit",
                "reason" to "Official project submission",
                "department" to "B.Tech EC",
                "role" to "Student",
                "phone" to "9876543214",
                "uid" to "0187EC211030",
                "batch" to "2021-2025",
                "campus" to "SISTec Gandhi Nagar",
                "applyDate" to todayDateStr,
                "departureTime" to "03:15 PM",
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
                "meetTo" to "Dr. S. K. Roy (HOD)",
                "status" to "meet",
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
                "meetTo" to "Principal Office",
                "status" to "pending",
                "campus" to "SISTec Gandhi Nagar",
                "entryDate" to todayDateStr,
                "img" to ""
            )
        )
    } else visitors

    // Filter by live search query
    val filteredPasses = displayPasses.filter { pass ->
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) true
        else {
            (pass["name"]?.lowercase()?.contains(q) == true) ||
            (pass["uid"]?.lowercase()?.contains(q) == true) ||
            (pass["gatePassId"]?.lowercase()?.contains(q) == true) ||
            (pass["department"]?.lowercase()?.contains(q) == true)
        }
    }

    val filteredVisitors = displayVisitors.filter { visitor ->
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) true
        else {
            (visitor["name"]?.lowercase()?.contains(q) == true) ||
            (visitor["meetTo"]?.lowercase()?.contains(q) == true) ||
            (visitor["phone"]?.lowercase()?.contains(q) == true) ||
            (visitor["visitorId"]?.lowercase()?.contains(q) == true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 120.dp)
    ) {
        // 1. Live Search Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color.White,
                elevation = 3.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = if (selectedTab == "Gate Pass") "Search student by name or UID..." else "Search visitor by name / host...",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.5.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = ThemeLogoBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = "Clear",
                                tint = Color.Gray,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = ThemeLogoBlue
                    )
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // 2. Segmented Toggle (Gate Pass vs Visitors)
        item {
            SegmentedToggle(
                options = listOf("Gate Pass", "Visitors"),
                selected = selectedTab,
                onSelect = { selectedTab = it }
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        // 3. Dynamic Stats Counters
        item {
            if (selectedTab == "Gate Pass") {
                val approvedCount = displayPasses.count { it["status"]?.lowercase() == "approved" }
                val exitedCount = displayPasses.count { it["status"]?.lowercase() in listOf("exit", "exited", "exited from source campus") }
                val pendingCount = displayPasses.count { it["status"]?.lowercase() in listOf("pending", "approving") }

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(Modifier.weight(1f), approvedCount.toString(), "Ready Exit", ThemeLightGreen)
                    StatBox(Modifier.weight(1f), exitedCount.toString(), "Exited", ThemeDarkBlue)
                    StatBox(Modifier.weight(1f), pendingCount.toString(), "Pending", ThemeGoldenOrange)
                }
            } else {
                val totalVisitors = displayVisitors.size
                val meetingCount = displayVisitors.count { it["status"]?.lowercase() == "meet" }
                val pendingCount = displayVisitors.count { it["status"]?.lowercase() == "pending" }

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(Modifier.weight(1f), totalVisitors.toString(), "Total", ThemeDarkBlue)
                    StatBox(Modifier.weight(1f), meetingCount.toString(), "Meeting", ThemeLightGreen)
                    StatBox(Modifier.weight(1f), pendingCount.toString(), "Pending", ThemeGoldenOrange)
                }
            }
        }

        // 4. Quick Action Button for Visitor Entry
        if (selectedTab == "Visitors") {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clickable { onNewVisitorEntry() },
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = ThemeLightGreen,
                    elevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = "New Visitor", tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("New Visitor Entry", color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 5. Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (selectedTab == "Gate Pass") "Approved Passes (Tap to Exit)" else "Visitor List",
                    color = Color(0xFF1C1F2E),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${if (selectedTab == "Gate Pass") filteredPasses.size else filteredVisitors.size} Found",
                    color = ThemeLogoBlue,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 6. List of Real-time Verification Cards
        if (selectedTab == "Gate Pass") {
            if (filteredPasses.isEmpty()) {
                item {
                    Text(
                        if (searchQuery.isNotEmpty()) "No gate passes match '$searchQuery'" else "No gate passes ready for exit.",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                filteredPasses.forEach { pass ->
                    val status = pass["status"]?.lowercase() ?: "pending"
                    item {
                        UserPhotoCard(
                            name = "${pass["name"] ?: "Student"} (${pass["uid"] ?: ""})",
                            status = status,
                            imageUrl = pass["img"] ?: pass["profilePic"] ?: pass["imageUrl"],
                            onClick = {
                                val intent = android.content.Intent(context, com.example.digitalpass.GatePassDetail::class.java)
                                intent.putExtra("gatePass", java.util.HashMap(pass))
                                intent.putExtra("operationType", "security")
                                intent.putExtra("listType", "recent")
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        } else {
            if (filteredVisitors.isEmpty()) {
                item {
                    Text(
                        if (searchQuery.isNotEmpty()) "No visitors match '$searchQuery'" else "No visitor appointments.",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                filteredVisitors.forEach { visitor ->
                    item {
                        UserPhotoCard(
                            name = "${visitor["name"] ?: "Visitor"} (To: ${visitor["meetTo"] ?: "Authority"})",
                            status = visitor["status"] ?: "meet",
                            imageUrl = visitor["img"] ?: visitor["photo"],
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
fun ReceptionDashboardBody(
    onNewVisitorEntry: () -> Unit = {},
    visitors: List<Map<String, String>> = emptyList()
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val totalVisitors = visitors.size
                val uniqueHosts = visitors.mapNotNull { it["meetTo"] }.distinct().size
                StatBox(Modifier.weight(1f), totalVisitors.toString(), "Visitors", ThemeDarkBlue)
                StatBox(Modifier.weight(1f), uniqueHosts.toString(), "Hosts", ThemeLightGreen)
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable { onNewVisitorEntry() },
                shape = RoundedCornerShape(16.dp),
                backgroundColor = ThemeLightGreen,
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.AddCircle, contentDescription = "New", tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("New Walk-in Visitor", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
        item {
            Text("Today's Appointments", color = Color(0xFF1C1F2E), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            if (visitors.isEmpty()) {
                Text("No appointments yet.", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                visitors.take(5).forEach { visitor ->
                    val name = visitor["name"] ?: "Visitor"
                    val meetTo = visitor["meetTo"] ?: "Someone"
                    ActionListItem("$name (Meeting with $meetTo)", Icons.Outlined.Person, ThemeLogoBlue)
                }
            }
        }
    }
}

@Composable
fun ManagementDashboardBody(
    role: String,
    onNavigate: (String) -> Unit = {},
    onOpenApprovalsTab: (String) -> Unit = {},
    gatePasses: List<Map<String, String>> = emptyList(),
    visitors: List<Map<String, String>> = emptyList()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedTab by remember { mutableStateOf("Gate Pass") }
    val roleLower = role.lowercase()
    val userEmail = com.example.digitalpass.LoginUserDataHolder.loginUserData?.get("email")?.lowercase() ?: ""
    val userName = com.example.digitalpass.LoginUserDataHolder.loginUserData?.get("name") ?: ""

    // Check if the current management user has applied for their own pass
    val myActivePass = gatePasses.firstOrNull { pass ->
        val passEmail = (pass["email"] ?: pass["applyEmail"] ?: "").lowercase()
        val passName = pass["name"] ?: ""
        (passEmail == userEmail && userEmail.isNotEmpty() || (passName.isNotEmpty() && passName == userName)) &&
                pass["status"]?.lowercase() in listOf("pending", "approving", "approved", "exit")
    }

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
                "name" to "Aman Sharma",
                "applyEmail" to "aman.sharma.student@sistec.ac.in",
                "email" to "aman.sharma.student@sistec.ac.in",
                "status" to "pending",
                "reason" to "Doctor appointment for regular medical checkup",
                "department" to "B.Tech CSE",
                "role" to "Student",
                "phone" to "9876543210",
                "fatherphone" to "9876543211",
                "fathername" to "Mr. Rajendra Sharma",
                "uid" to "0187CS211045",
                "batch" to "2021-2025",
                "campus" to "SISTec Gandhi Nagar",
                "applyDate" to todayDateStr,
                "departureTime" to "04:30 PM",
                "img" to ""
            ),
            mapOf(
                "gatePassId" to "GP-1002",
                "name" to "Priya Singh",
                "applyEmail" to "priya.singh.student@sistec.ac.in",
                "email" to "priya.singh.student@sistec.ac.in",
                "status" to "approving",
                "tgRemark" to "Verified with parents, genuine reason.",
                "reason" to "Going Home for family occasion",
                "department" to "B.Tech ME",
                "role" to "Student",
                "phone" to "9876543212",
                "fatherphone" to "9876543213",
                "fathername" to "Mr. Suresh Singh",
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp)
    ) {
        // 1. My Active Pass Widget (If Management Member Applied)
        if (myActivePass != null) {
            item {
                Text("My Active Pass Ticket", color = Color(0xFF1C1F2E), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))
                ActivePassTicketWidget(myActivePass, onClick = {
                    val intent = android.content.Intent(context, com.example.digitalpass.GatePassDetail::class.java)
                    intent.putExtra("gatePass", java.util.HashMap(myActivePass))
                    intent.putExtra("operationType", "self")
                    intent.putExtra("listType", "recent")
                    context.startActivity(intent)
                })
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 2. Segmented Toggle
        item {
            SegmentedToggle(
                options = listOf("Gate Pass", "Visitors"),
                selected = selectedTab,
                onSelect = { selectedTab = it }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 3. Dynamic Stats Row (Pending, Approving, Approved / Visitors)
        item {
            if (selectedTab == "Gate Pass") {
                val pendingCount = displayPasses.count { it["status"]?.lowercase() == "pending" }
                val approvingCount = displayPasses.count { it["status"]?.lowercase() == "approving" }
                val approvedCount = displayPasses.count { it["status"]?.lowercase() == "approved" }
                val exitedCount = displayPasses.count { it["status"]?.lowercase() in listOf("exit", "exited", "exited from source campus") }

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(Modifier.weight(1f).clickable { onOpenApprovalsTab("Gate Pass") }, pendingCount.toString(), "Pending", ThemeGoldenOrange)
                    StatBox(Modifier.weight(1f).clickable { onOpenApprovalsTab("Gate Pass") }, approvingCount.toString(), "Approving", ThemeDarkBlue)
                    StatBox(Modifier.weight(1f).clickable { onOpenApprovalsTab("Gate Pass") }, (approvedCount + exitedCount).toString(), "Approved", ThemeLightGreen)
                }
            } else {
                val totalVisitors = displayVisitors.size
                val meetingCount = displayVisitors.count { it["status"]?.lowercase() == "meet" }
                val pendingVisitors = displayVisitors.count { it["status"]?.lowercase() == "pending" }

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(Modifier.weight(1f).clickable { onOpenApprovalsTab("Visitor") }, totalVisitors.toString(), "Total", ThemeDarkBlue)
                    StatBox(Modifier.weight(1f).clickable { onOpenApprovalsTab("Visitor") }, pendingVisitors.toString(), "Pending", ThemeGoldenOrange)
                    StatBox(Modifier.weight(1f).clickable { onOpenApprovalsTab("Visitor") }, meetingCount.toString(), "Meeting", ThemeLightGreen)
                }
            }
        }

        // 4. Quick Actions
        item {
            Text("Quick Actions", color = Color(0xFF1C1F2E), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallActionCard(Modifier.weight(1f), "Apply Pass", Icons.Outlined.AddCircle) { onNavigate("apply") }
                SmallActionCard(Modifier.weight(1f), "User Mgmt", Icons.Outlined.Person) { onNavigate("users") }
                if (roleLower in listOf("admin", "principal", "hod")) {
                    SmallActionCard(Modifier.weight(1f), "Batches", Icons.Outlined.DateRange) { onNavigate("batches") }
                } else {
                    SmallActionCard(Modifier.weight(1f), "History", Icons.Outlined.DateRange) { onNavigate("history") }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (roleLower in listOf("admin", "principal")) {
                    SmallActionCard(Modifier.weight(1f), "Campus", Icons.Outlined.LocationOn) { onNavigate("campus") }
                }
                SmallActionCard(Modifier.weight(1f), "Reports", Icons.Outlined.Info) { onNavigate("report") }
                SmallActionCard(Modifier.weight(1f), "History", Icons.Outlined.DateRange) { onNavigate("history") }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // 5. Content: Pending Approvals or Visitor List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (selectedTab == "Gate Pass") "Pending Approvals" else "Visitor Appointments",
                    color = Color(0xFF1C1F2E), fontSize = 18.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    "View All →",
                    color = ThemeLogoBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        if (selectedTab == "Gate Pass") onOpenApprovalsTab("Gate Pass")
                        else onOpenApprovalsTab("Visitor")
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (selectedTab == "Gate Pass") {
            val pendingPasses = displayPasses.filter { it["status"]?.lowercase() in listOf("pending", "approving") }
            if (pendingPasses.isEmpty()) {
                item {
                    Text("No pending gate passes.", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                }
            } else {
                pendingPasses.take(5).forEach { pass ->
                    item {
                        UserPhotoCard(
                            name = pass["name"] ?: "Student",
                            status = pass["status"] ?: "pending",
                            imageUrl = pass["img"] ?: pass["profilePic"] ?: pass["imageUrl"],
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
            }
        } else {
            val pendingVisitors = displayVisitors.filter { it["status"]?.lowercase() in listOf("pending", "approving", "entered", "meet") }
            if (pendingVisitors.isEmpty()) {
                item {
                    Text("No visitor appointments.", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                }
            } else {
                pendingVisitors.take(5).forEach { visitor ->
                    item {
                        UserPhotoCard(
                            name = visitor["name"] ?: "Visitor",
                            status = visitor["status"] ?: "meet",
                            imageUrl = visitor["img"] ?: visitor["photo"],
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
fun PendingApprovalCard(name: String, type: String, status: String) {
    val statusLower = status.lowercase()
    val statusColor = when (statusLower) {
        "pending" -> ThemeGoldenOrange
        "approving" -> ThemeDarkBlue
        "approved" -> ThemeLightGreen
        "exit", "exited" -> Color(0xFF64748B)
        "rejected" -> Color(0xFFEF5350)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = Color.White,
        elevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        status.replaceFirstChar { it.uppercase() },
                        color = statusColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Reason: $type", color = Color(0xFF64748B), fontSize = 13.sp)
        }
    }
}

@Composable
fun ActivePassTicketWidget(pass: Map<String, String>, onClick: () -> Unit = {}) {
    val status = pass["status"]?.lowercase() ?: "pending"
    val reason = pass["reason"] ?: "Gate Pass"
    val time = pass["departureTime"] ?: "Just now"
    val campus = pass["destinationCampus"]?.takeIf { it.isNotEmpty() } ?: pass["campus"] ?: "Local Campus"
    val isStudent = pass["role"]?.lowercase() == "student"

    // 4 Distinct Visual Themes for pending, approving, approved, and exit
    val (statusBg, statusBorder, statusText, statusTitle, statusIcon, statusSubtitle) = when (status) {
        "approved" -> Tuple6(
            Color(0xFFECFDF5),
            Color(0xFFA7F3D0),
            Color(0xFF059669),
            "APPROVED • READY FOR EXIT",
            "🎫",
            "Valid Departure: $time • Show at Gate to Exit"
        )
        "approving" -> Tuple6(
            Color(0xFFEEF2FF),
            Color(0xFFC7D2FE),
            Color(0xFF4F46E5),
            "IN REVIEW • TG APPROVED",
            "🔄",
            "TG Remark Added • Forwarded for Final HOD Approval"
        )
        "exit", "exited", "exited from source campus" -> Tuple6(
            Color(0xFFF1F5F9),
            Color(0xFFCBD5E1),
            Color(0xFF475569),
            "GATE EXIT COMPLETED",
            "🚪",
            "Exited Campus • Security Scan Completed"
        )
        else -> Tuple6(
            Color(0xFFFFFBEB),
            Color(0xFFFDE68A),
            Color(0xFFD97706),
            "PENDING APPROVAL",
            "⏳",
            if (isStudent) "Applied: $time • Under TG / Authority Review" else "Applied: $time • Under HOD / Principal Review"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        backgroundColor = Color.White,
        elevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, statusBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Status Header Banner
            Surface(
                color = statusBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(statusText, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusTitle,
                            color = statusText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Icon(
                        imageVector = if (status == "approved") Icons.Filled.CheckCircle else Icons.Filled.Info,
                        contentDescription = null,
                        tint = statusText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Ticket Body
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Container
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = statusBg,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = statusIcon,
                            fontSize = 26.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reason,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.5.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📍 $campus",
                        color = ThemeLogoBlue,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = statusSubtitle,
                        color = Color(0xFF64748B),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

data class Tuple6<A, B, C, D, E, F>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E,
    val f: F
)
