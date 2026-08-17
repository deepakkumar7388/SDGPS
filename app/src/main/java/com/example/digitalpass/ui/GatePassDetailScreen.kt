package com.example.digitalpass.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.digitalpass.LoginUserDataHolder

@Composable
fun GatePassDetailScreen(
    gatePass: Map<String, String>,
    operationType: String,
    listType: String,
    isLoading: Boolean,
    isActionAllowed: Boolean,
    previousPasses: List<Map<String, String>>,
    onBack: () -> Unit,
    onCallPhone: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onApprove: (tgRemark: String) -> Unit,
    onReject: () -> Unit,
    onEditSave: (newReason: String, newTgRemark: String) -> Unit,
    onRemoveSelfPass: () -> Unit,
    onSecurityAction: () -> Unit,
    onActivateInterPass: () -> Unit,
    onLoadPreviousPasses: () -> Unit
) {
    val role = LoginUserDataHolder.loginUserData?.get("role")?.lowercase()?.trim() ?: ""
    val isInterInstitutional = (!gatePass["destinationCampus"].isNullOrBlank()) || gatePass["passType"]?.contains("inter", ignoreCase = true) == true
    val statusVal = (gatePass["status"] ?: "pending").lowercase().trim()

    var isEditMode by remember { mutableStateOf(false) }
    var liveTgRemark by remember(gatePass["tgRemark"]) { mutableStateOf(gatePass["tgRemark"] ?: "") }
    var editableReason by remember(gatePass["reason"]) { mutableStateOf(gatePass["reason"] ?: "") }

    var showTgRemarkDialog by remember { mutableStateOf(false) }
    var dialogRemarkText by remember { mutableStateOf("") }
    var showAdditionalDetails by remember { mutableStateOf(false) }
    var showHistorySection by remember { mutableStateOf(false) }
    var selectedHistoryPass by remember { mutableStateOf<Map<String, String>?>(null) }

    val userEmail = LoginUserDataHolder.loginUserData?.get("email")?.lowercase()?.trim() ?: ""
    val passEmail = (gatePass["email"] ?: gatePass["applyEmail"] ?: "").lowercase().trim()
    val isSelfPass = operationType == "self" || (userEmail.isNotBlank() && passEmail.isNotBlank() && passEmail == userEmail)
    val applicantPassRole = (gatePass["role"] ?: "student").lowercase().trim()
    val isSecurityGuard = (role == "security guard" || operationType == "security") && !isSelfPass
    val canEdit = (isSelfPass && statusVal == "pending") || (!isSelfPass && isActionAllowed && !isSecurityGuard)

    // Construct exact other information description like original XML
    val otherInfoText = buildString {
        append("Campus : ${gatePass["campus"] ?: "SISTec"}")
        if (gatePass["role"]?.lowercase() == "student" && gatePass["applyEmail"] != LoginUserDataHolder.loginUserData?.get("email")) {
            if (!gatePass["uid"].isNullOrBlank()) append("\nUID : ${gatePass["uid"]}")
            if (!gatePass["batch"].isNullOrBlank()) append("\nBatch : ${gatePass["batch"]}")
            if (!gatePass["fathername"].isNullOrBlank()) append("\nFather Name : ${gatePass["fathername"]}")
        }
        if (!gatePass["applyDate"].isNullOrBlank()) append("\nApply Date : ${gatePass["applyDate"]}")
        if (!gatePass["remark"].isNullOrBlank()) append("\nRemarks : ${gatePass["remark"]}")
    }

    Scaffold(
        backgroundColor = Color(0xFFF8FAFC),
        topBar = {
            Surface(
                color = Color.White,
                elevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF1C1F2E),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSecurityGuard) "Gate Exit Verification" else if (isInterInstitutional) "Inter-Campus Pass Details" else "Regular Pass Details",
                            color = Color(0xFF1C1F2E),
                            fontSize = 18.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    if (canEdit) {
                        IconButton(
                            onClick = { isEditMode = !isEditMode },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isEditMode) Icons.Default.Close else Icons.Outlined.Edit,
                                contentDescription = if (isEditMode) "Close Edit" else "Edit Pass",
                                tint = if (isEditMode) Color(0xFFDC2626) else ThemeLogoBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (isActionAllowed && listType != "history") {
                Surface(
                    color = Color.White,
                    elevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isSelfPass && role !in listOf("hod", "principal", "admin")) {
                            // ━━━ SELF (Any role EXCEPT HOD/Principal viewing their OWN pass) ━━━
                            if (statusVal.lowercase() == "pending") {
                                Button(
                                    onClick = onRemoveSelfPass,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFEE2E2)),
                                    elevation = ButtonDefaults.elevation(0.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Remove Pass", color = Color(0xFFDC2626), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (isSecurityGuard) {
                            // ━━━ SECURITY GUARD ━━━
                            var guardActionText = "Allow Gate Exit"
                            if (isInterInstitutional) {
                                val myCampus = LoginUserDataHolder.loginUserData?.get("campus")
                                if (gatePass["campus"] == myCampus) {
                                    if (statusVal != "approved") guardActionText = "🏢 Re-entered into Source Campus"
                                    else guardActionText = "Exit from Source Campus"
                                } else {
                                    if (statusVal == "Exited from source campus") guardActionText = "🏢 Enter Destination Campus"
                                    else guardActionText = "Exit from Destination Campus"
                                }
                            }

                            Button(
                                onClick = onSecurityAction,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = ThemeLogoBlue),
                                elevation = ButtonDefaults.elevation(4.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(guardActionText, color = Color.White, fontSize = 16.5.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        } else if (role in listOf("faculty", "management member", "teacher", "tg") && (applicantPassRole == "student" || applicantPassRole == "candidate")) {
                            // ━━━ FACULTY / TG reviewing a STUDENT pass ━━━
                            // Must add TG Remark, then forward to HOD
                            Button(
                                onClick = onReject,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFEE2E2)),
                                elevation = ButtonDefaults.elevation(0.dp)
                            ) {
                                Text("✕ Reject", color = Color(0xFFDC2626), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (liveTgRemark.isNotBlank()) {
                                        onApprove(liveTgRemark)
                                    } else {
                                        showTgRemarkDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF7C3AED)),
                                elevation = ButtonDefaults.elevation(2.dp)
                            ) {
                                Text("📝 Forward to HOD", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // ━━━ HOD / PRINCIPAL / ADMIN / Faculty reviewing non-student ━━━
                            // Direct Approve or Reject
                            Button(
                                onClick = onReject,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFEE2E2)),
                                elevation = ButtonDefaults.elevation(0.dp)
                            ) {
                                Text("✕ Reject Pass", color = Color(0xFFDC2626), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    onApprove(liveTgRemark)
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF059669)),
                                elevation = ButtonDefaults.elevation(2.dp)
                            ) {
                                val isHodOrPrincipal = role in listOf("hod", "principal", "admin")
                                val approveButtonText = if (isHodOrPrincipal && (applicantPassRole == "student" || applicantPassRole == "candidate")) "✓ Final Approve" else "✓ Approve Pass"
                                Text(approveButtonText, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isSecurityGuard) {
                // DEDICATED SECURITY GUARD VERIFICATION UI
                SecurityGuardPassView(
                    gatePass = gatePass,
                    onCallPhone = onCallPhone,
                    onImageClick = onImageClick,
                    isInter = isInterInstitutional
                )
            } else {
                // AUTHORITY & STUDENT DETAIL VIEW
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. HERO PROFILE & IDENTITY CARD
                    item {
                        StudentHeroCard(
                            gatePass = gatePass,
                            onImageClick = { onImageClick(gatePass["img"] ?: "") }
                        )
                    }

                    // 2. PASS LIFECYCLE PROGRESS TRACKER (TYPE-SPECIFIC)
                    item {
                        PassLifecycleCard(gatePass = gatePass, isInter = isInterInstitutional, operationType = operationType)
                    }

                    // Activate Pass button for Inter-Campus Student
                    if (isInterInstitutional && statusVal.lowercase() in listOf("entered into destination campus", "entered into destination") && gatePass["passActivity"] != "active" && isSelfPass) {
                        item {
                            Button(
                                onClick = onActivateInterPass,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF059669)),
                                elevation = ButtonDefaults.elevation(3.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("⚡ Activate Pass at Destination Campus", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 3. PASS TYPE SPECIFIC INFORMATION CARD
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = Color.White,
                            elevation = 0.dp,
                            border = BorderStroke(1.dp, Color(0xFFEDF2F7))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isInterInstitutional) "Inter-Campus Movement Details" else "Pass Information",
                                        color = Color(0xFF1C1F2E),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = if (isInterInstitutional) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = if (isInterInstitutional) "🏛️ Inter-Campus" else "🎫 Regular Exit",
                                            color = if (isInterInstitutional) ThemeLogoBlue else Color(0xFF475569),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Inter-Campus Route Visualizer
                                if (isInterInstitutional) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        backgroundColor = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        elevation = 0.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("FROM (SOURCE)", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(gatePass["campus"] ?: "SISTec Gandhi Nagar", color = Color(0xFF1E293B), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text(" ──► ", color = ThemeLogoBlue, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                                Text("TO (DESTINATION)", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(gatePass["destinationCampus"] ?: "SISTec Ratibad", color = ThemeLogoBlue, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                }

                                // Departure Time Info Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Departure Time", color = Color(0xFF64748B), fontSize = 11.5.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(gatePass["departureTime"] ?: "N/A", color = Color(0xFF1C1F2E), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Campus", color = Color(0xFF64748B), fontSize = 11.5.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(gatePass["campus"] ?: "SISTec", color = Color(0xFF1C1F2E), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Divider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(14.dp))

                                // Reason for pass
                                if (isEditMode && isSelfPass && statusVal.lowercase() == "pending") {
                                    OutlinedTextField(
                                        value = editableReason,
                                        onValueChange = { editableReason = it },
                                        label = { Text("Edit Reason for Gate Pass", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1C1F2E),
                                            lineHeight = 24.sp
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = ThemeLogoBlue,
                                            focusedLabelColor = ThemeLogoBlue
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            onEditSave(editableReason, liveTgRemark)
                                            isEditMode = false
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(backgroundColor = ThemeLogoBlue),
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save Changes", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text(
                                        text = "PURPOSE OF GATE PASS",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = gatePass["reason"] ?: "Not specified",
                                        color = Color(0xFF1E293B),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 22.sp
                                    )
                                }

                                // General Remark (if entered during application)
                                if (!gatePass["remark"].isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider(color = Color(0xFFF1F5F9))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "APPLICANT REMARKS",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = gatePass["remark"]!!,
                                        color = Color(0xFF475569),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }

                // 4. TEACHER / TG REMARK CARD (Exact Old Repo Logic: Only for Student Passes)
                val isFacultyTg = role in listOf("faculty", "management member")
                val isHodPrincipal = role in listOf("hod", "principal", "admin")

                // Old Repo Rule: TG Remark is ONLY applicable for Students.
                // Shown when: 1) Faculty is reviewing student pass, OR 2) A TG remark actually exists.
                if (applicantPassRole == "student" && (liveTgRemark.isNotBlank() || (isFacultyTg && isActionAllowed && !isSelfPass))) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = Color.White,
                            elevation = 0.dp,
                            border = BorderStroke(1.dp, if (isFacultyTg && liveTgRemark.isBlank() && isActionAllowed) Color(0xFFFDE68A) else Color(0xFFEDF2F7))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isFacultyTg && isActionAllowed && !isSelfPass) "TG Remark (Required)" else "Teacher / TG Remark",
                                        color = Color(0xFF1C1F2E),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isFacultyTg && isActionAllowed && !isSelfPass) {
                                        Surface(
                                            color = Color(0xFFFFFBEB),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "⚠ Mandatory",
                                                color = Color(0xFFD97706),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (isFacultyTg && isActionAllowed && !isSelfPass) {
                                    // Faculty/TG: Editable field when reviewing student pass
                                    OutlinedTextField(
                                        value = liveTgRemark,
                                        onValueChange = { liveTgRemark = it },
                                        label = { Text("Write your remark before forwarding to HOD", fontSize = 13.sp) },
                                        placeholder = { Text("e.g. Student is genuine, reason verified", fontSize = 14.sp) },
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1C1F2E),
                                            lineHeight = 24.sp
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = Color(0xFF7C3AED),
                                            focusedLabelColor = Color(0xFF7C3AED)
                                        )
                                    )
                                    if (liveTgRemark.isBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "⚠ You must add a remark before forwarding this pass to HOD",
                                            color = Color(0xFFD97706),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else if (isEditMode && isHodPrincipal && isActionAllowed && !isSelfPass) {
                                    // HOD/Principal in edit mode: Can optionally update TG remark
                                    OutlinedTextField(
                                        value = liveTgRemark,
                                        onValueChange = { liveTgRemark = it },
                                        label = { Text("Teacher / TG Remark (Optional Edit)", fontSize = 13.sp) },
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1C1F2E),
                                            lineHeight = 24.sp
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = ThemeLogoBlue,
                                            focusedLabelColor = ThemeLogoBlue
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            onEditSave(editableReason, liveTgRemark)
                                            isEditMode = false
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(backgroundColor = ThemeLogoBlue),
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save Remark", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    // Read-only view when remark exists
                                    Surface(
                                        color = Color(0xFFF0F9FF),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "TEACHER'S REMARK",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "\"$liveTgRemark\"",
                                                color = Color(0xFF0A58CA),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                lineHeight = 22.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. CONTACT VERIFICATION & PARENT CALLING CARD
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = Color.White,
                        elevation = 0.dp,
                        border = BorderStroke(1.dp, Color(0xFFEDF2F7))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Contact Verification",
                                color = Color(0xFF1C1F2E),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Candidate Phone
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Candidate Phone", color = Color(0xFF64748B), fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(gatePass["phone"] ?: "N/A", color = Color(0xFF1C1F2E), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }

                                val candidatePhone = gatePass["phone"] ?: ""
                                if (candidatePhone.isNotBlank()) {
                                    Button(
                                        onClick = { onCallPhone(candidatePhone) },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFEFF6FF)),
                                        elevation = ButtonDefaults.elevation(0.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text("Call", color = ThemeLogoBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Parent Phone (if available)
                            val parentPhone = gatePass["fatherphone"] ?: ""
                            if (parentPhone.isNotBlank() && role != "student") {
                                Spacer(modifier = Modifier.height(14.dp))
                                Divider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Parent / Guardian Phone", color = Color(0xFF64748B), fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(parentPhone, color = Color(0xFF1C1F2E), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { onCallPhone(parentPhone) },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFEFF6FF)),
                                        elevation = ButtonDefaults.elevation(0.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text("Call Parent", color = ThemeLogoBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. ADDITIONAL DETAILS ACCORDION
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = Color.White,
                        elevation = 0.dp,
                        border = BorderStroke(1.dp, Color(0xFFEDF2F7))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAdditionalDetails = !showAdditionalDetails },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Additional Details",
                                    color = Color(0xFF1C1F2E),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = if (showAdditionalDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B)
                                )
                            }

                            AnimatedVisibility(visible = showAdditionalDetails) {
                                Text(
                                    text = otherInfoText,
                                    color = Color(0xFF475569),
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                        }
                    }
                }

                // 7. ALL APPLIED GATE PASSES (HISTORY ACCORDION)
                if (!isSelfPass) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = Color.White,
                            elevation = 0.dp,
                            border = BorderStroke(1.dp, Color(0xFFEDF2F7))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showHistorySection = !showHistorySection
                                            if (showHistorySection && previousPasses.isEmpty()) {
                                                onLoadPreviousPasses()
                                            }
                                        },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.List,
                                            contentDescription = null,
                                            tint = ThemeLogoBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "All Applied Gate Passes",
                                            color = Color(0xFF1C1F2E),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Icon(
                                        imageVector = if (showHistorySection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                }

                                AnimatedVisibility(visible = showHistorySection) {
                                    Column(modifier = Modifier.padding(top = 14.dp)) {
                                        if (previousPasses.isEmpty()) {
                                            Text("No past gate passes found.", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                                        } else {
                                            previousPasses.forEach { pastPass ->
                                                Surface(
                                                    color = Color(0xFFF8FAFC),
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp)
                                                        .clickable { selectedHistoryPass = pastPass }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(pastPass["applyDate"] ?: "Date N/A", color = Color(0xFF1C1F2E), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                            Text(pastPass["reason"] ?: "Gate Pass", color = Color(0xFF64748B), fontSize = 12.sp)
                                                        }
                                                        Surface(
                                                            color = if (pastPass["status"]?.lowercase() == "approved") Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                                            shape = RoundedCornerShape(10.dp)
                                                        ) {
                                                            Text(
                                                                text = pastPass["status"]?.uppercase() ?: "PENDING",
                                                                color = if (pastPass["status"]?.lowercase() == "approved") Color(0xFF15803D) else Color(0xFFD97706),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // High-precision Loading Overlay
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
        }
    }

    // TG Remark Dialog
    if (showTgRemarkDialog) {
        AlertDialog(
            onDismissRequest = { showTgRemarkDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Teacher / TG Remark", color = Color(0xFF1C1F2E), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Please enter remarks before giving final approval:", color = Color(0xFF64748B), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = dialogRemarkText,
                        onValueChange = { dialogRemarkText = it },
                        label = { Text("Remark") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                        if (dialogRemarkText.isNotBlank()) {
                            showTgRemarkDialog = false
                            liveTgRemark = dialogRemarkText
                            onApprove(dialogRemarkText)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = ThemeLogoBlue)
                ) {
                    Text("Approve", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTgRemarkDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Past Pass Detail Dialog
    if (selectedHistoryPass != null) {
        val pass = selectedHistoryPass!!
        AlertDialog(
            onDismissRequest = { selectedHistoryPass = null },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Gate Pass Details", color = Color(0xFF1C1F2E), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Date & Time: ${pass["applyDate"] ?: "N/A"}", color = Color(0xFF1C1F2E), fontSize = 14.sp)
                    Text("Status: ${pass["status"] ?: "N/A"}", color = Color(0xFF1C1F2E), fontSize = 14.sp)
                    if (!pass["campus"].isNullOrBlank()) Text("Campus: ${pass["campus"]}", color = Color(0xFF1C1F2E), fontSize = 14.sp)
                    Text("Reason: ${pass["reason"] ?: "N/A"}", color = Color(0xFF1C1F2E), fontSize = 14.sp)
                    if (!pass["tgRemark"].isNullOrBlank()) Text("TG Remark: ${pass["tgRemark"]}", color = Color(0xFF1C1F2E), fontSize = 14.sp)
                    if (!pass["remark"].isNullOrBlank()) Text("Remarks: ${pass["remark"]}", color = Color(0xFF1C1F2E), fontSize = 14.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedHistoryPass = null },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = ThemeLogoBlue)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun StudentHeroCard(
    gatePass: Map<String, String>,
    onImageClick: () -> Unit
) {
    val statusVal = gatePass["status"] ?: "pending"
    val isInter = !gatePass["destinationCampus"].isNullOrBlank()
    val imgUrl = gatePass["img"] ?: gatePass["profilePic"] ?: gatePass["imageUrl"]

    val statusColor = when (statusVal.lowercase()) {
        "approved" -> Color(0xFF15803D) to Color(0xFFDCFCE7)
        "rejected" -> Color(0xFFDC2626) to Color(0xFFFEE2E2)
        "exit", "exited" -> Color(0xFF0369A1) to Color(0xFFE0F2FE)
        "expired" -> Color(0xFF64748B) to Color(0xFFF1F5F9)
        else -> Color(0xFFD97706) to Color(0xFFFEF3C7)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = Color.White,
        elevation = 0.dp,
        border = BorderStroke(1.dp, Color(0xFFEDF2F7))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2E8F0))
                        .clickable { onImageClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (!imgUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = LoginUserDataHolder.getURL(imgUrl),
                            contentDescription = "Student Photo",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(ThemeLogoBlue.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (gatePass["name"]?.take(1) ?: "S").uppercase(),
                                color = ThemeLogoBlue,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = gatePass["name"] ?: "Student Name",
                        color = Color(0xFF1C1F2E),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Status Pill
                        Surface(
                            color = statusColor.second,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = if (statusVal.equals("approving", true)) "IN PROCESS" else statusVal.uppercase(),
                                color = statusColor.first,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }

                        // Pass Type Pill
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = if (isInter) "Inter-Campus" else "Regular",
                                color = ThemeLogoBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(14.dp))

            // Pass ID & Department
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pass ID", color = Color(0xFF64748B), fontSize = 12.sp)
                    Text(gatePass["gatePassId"] ?: "N/A", color = Color(0xFF1C1F2E), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Department", color = Color(0xFF64748B), fontSize = 12.sp)
                    Text("${gatePass["department"] ?: ""} • ${gatePass["role"] ?: ""}", color = Color(0xFF1C1F2E), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PassLifecycleCard(
    gatePass: Map<String, String>,
    isInter: Boolean,
    operationType: String = "member"
) {
    val statusVal = gatePass["status"]?.lowercase()?.trim() ?: "pending"
    val loggedInRole = LoginUserDataHolder.loginUserData?.get("role")?.lowercase()?.trim() ?: ""

    // Robust applicant role detection:
    // If self pass, fallback to logged-in user's role if pass role is missing
    val applicantRole = when {
        !gatePass["role"].isNullOrBlank() -> gatePass["role"]!!.lowercase().trim()
        operationType == "self" && loggedInRole.isNotBlank() -> loggedInRole
        else -> "student"
    }

    // Check applicant category
    val isStudent = applicantRole == "student" || applicantRole == "candidate"
    val isFacultyStaff = applicantRole in listOf("faculty", "management member", "management", "teacher", "tg", "staff", "member")
    val isHod = applicantRole == "hod"
    val isGuard = applicantRole in listOf("security guard", "security", "guard")
    val isPrincipalAdmin = applicantRole in listOf("principal", "admin", "director")

    // Dynamically compute exact steps based on WHO applied for the pass & Pass Type
    val (steps, activeIndex) = when {
        // 1. Inter-Campus Pass
        isInter -> {
            if (isStudent) {
                // Student Inter-Campus: 6 Steps (Includes TG Review)
                val stepList = listOf("Applied", "TG Review", "HOD Appr.", "Src Exit", "Dst Entry", "Returned")
                val idx = when (statusVal) {
                    "pending" -> 1        // Applied done, waiting for TG
                    "approving" -> 2      // TG done, waiting for HOD
                    "approved" -> 3       // HOD done, ready for Exit
                    "exited from source campus" -> 4
                    "entered into destination campus", "exited from destination campus" -> 5
                    "entered into source campus", "returned" -> 6 // All completed
                    "rejected" -> -1
                    else -> 1
                }
                stepList to idx
            } else {
                // Faculty / Staff / HOD Inter-Campus: 5 Steps (NO TG Review - Direct HOD/Principal)
                val approver = if (isHod || isPrincipalAdmin) "Principal" else if (isGuard) "Admin" else "HOD"
                val stepList = listOf("Applied", "$approver Appr.", "Src Exit", "Dst Entry", "Returned")
                val idx = when (statusVal) {
                    "pending" -> 1        // Applied done, waiting for Approver
                    "approved" -> 2       // Approver done, ready for Exit
                    "exited from source campus" -> 3
                    "entered into destination campus", "exited from destination campus" -> 4
                    "entered into source campus", "returned" -> 5 // All completed
                    "rejected" -> -1
                    else -> 1
                }
                stepList to idx
            }
        }

        // 2. Regular Pass - Student: 4 Steps (Applied ➔ TG Review ➔ HOD Appr. ➔ Gate Exit)
        isStudent -> {
            val stepList = listOf("Applied", "TG Review", "HOD Appr.", "Gate Exit")
            val idx = when (statusVal) {
                "pending" -> 1        // Applied done, waiting for TG
                "approving" -> 2      // TG done, waiting for HOD
                "approved" -> 3       // HOD done, waiting for Gate Exit
                "exit", "exited" -> 4 // All 4 completed
                "rejected" -> -1
                else -> 1
            }
            stepList to idx
        }

        // 3. Regular Pass - Faculty / Management Staff / Teacher / TG: 3 Steps (Applied ➔ HOD Appr. ➔ Gate Exit)
        // NOTE: Faculty does NOT have a TG. Only HOD approves their pass!
        isFacultyStaff -> {
            val stepList = listOf("Applied", "HOD Appr.", "Gate Exit")
            val idx = when (statusVal) {
                "pending" -> 1        // Applied done, waiting for HOD
                "approved" -> 2       // HOD done, waiting for Gate Exit
                "exit", "exited" -> 3 // All 3 completed
                "rejected" -> -1
                else -> 1
            }
            stepList to idx
        }

        // 4. Regular Pass - HOD / Security Guard: 3 Steps (Applied ➔ Principal/Admin Appr. ➔ Gate Exit)
        isHod || isGuard -> {
            val approver = if (isGuard) "Admin" else "Principal"
            val stepList = listOf("Applied", "$approver Appr.", "Gate Exit")
            val idx = when (statusVal) {
                "pending" -> 1        // Applied done, waiting for approval
                "approved" -> 2       // Approved, waiting for Gate Exit
                "exit", "exited" -> 3 // All 3 completed
                "rejected" -> -1
                else -> 1
            }
            stepList to idx
        }

        // 5. Regular Pass - Principal / Admin: 2 Steps (Sanctioned ➔ Gate Exit)
        else -> {
            val stepList = listOf("Sanctioned", "Gate Exit")
            val idx = when (statusVal) {
                "pending" -> 1        // Sanctioned done, waiting for gate
                "approved" -> 1       // Approved, waiting for gate exit
                "exit", "exited" -> 2 // All completed
                "rejected" -> -1
                else -> 1
            }
            stepList to idx
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = Color.White,
        elevation = 0.dp,
        border = BorderStroke(1.dp, Color(0xFFEDF2F7))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pass Lifecycle", color = Color(0xFF1C1F2E), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = if (statusVal == "rejected") "REJECTED" else if (activeIndex >= steps.size) "COMPLETED" else "LIVE TRACKING",
                    color = if (statusVal == "rejected") Color(0xFFDC2626) else if (activeIndex >= steps.size) Color(0xFF15803D) else ThemeLogoBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Horizontal Stepper Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                steps.forEachIndexed { index, stepName ->
                    val isDone = activeIndex > index
                    val isActive = activeIndex == index && statusVal != "rejected"
                    val isRejected = statusVal == "rejected" && index == activeIndex

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isInter) 26.dp else 32.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDone -> Color(0xFF10B981)
                                        isRejected -> Color(0xFFDC2626)
                                        isActive -> ThemeLogoBlue
                                        else -> Color(0xFFE2E8F0)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isDone -> Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(if (isInter) 14.dp else 18.dp))
                                isRejected -> Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(if (isInter) 14.dp else 18.dp))
                                isActive -> Box(modifier = Modifier.size(if (isInter) 8.dp else 10.dp).clip(CircleShape).background(Color.White))
                                else -> Box(modifier = Modifier.size(if (isInter) 6.dp else 8.dp).clip(CircleShape).background(Color(0xFF94A3B8)))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stepName,
                            color = if (isActive || isDone) Color(0xFF1C1F2E) else Color(0xFF94A3B8),
                            fontSize = if (isInter) 9.sp else 11.sp,
                            fontWeight = if (isActive || isDone) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }

                    if (index < steps.size - 1) {
                        val lineDone = activeIndex > index
                        Box(
                            modifier = Modifier
                                .weight(if (isInter) 0.3f else 0.6f)
                                .height(3.dp)
                                .offset(y = (-10).dp)
                                .background(
                                    if (lineDone) Color(0xFF10B981) else Color(0xFFE2E8F0),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityGuardPassView(
    gatePass: Map<String, String>,
    onCallPhone: (String) -> Unit,
    onImageClick: (String) -> Unit,
    isInter: Boolean
) {
    val name = gatePass["name"] ?: "Person Name"
    val uid = gatePass["uid"] ?: ""
    val role = gatePass["role"]?.lowercase() ?: "student"
    val department = gatePass["department"] ?: "SISTec"
    val batch = gatePass["batch"] ?: ""
    val campus = gatePass["campus"] ?: "SISTec Gandhi Nagar"
    val destinationCampus = gatePass["destinationCampus"] ?: ""
    val reason = gatePass["reason"] ?: "Gate Pass"
    val departureTime = gatePass["departureTime"] ?: "N/A"
    val applyDate = gatePass["applyDate"] ?: ""
    val tgRemark = gatePass["tgRemark"] ?: ""
    val phone = gatePass["phone"] ?: ""
    val fatherName = gatePass["fathername"] ?: ""
    val fatherPhone = gatePass["fatherphone"] ?: ""
    val imageUrl = gatePass["img"] ?: gatePass["profilePic"] ?: ""
    val status = gatePass["status"] ?: "approved"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. TOP GATE APPROVAL BADGE
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                backgroundColor = Color(0xFFECFDF5),
                elevation = 0.dp,
                border = BorderStroke(1.5.dp, Color(0xFFA7F3D0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color(0xFF059669), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Approved",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (isInter) "APPROVED • INTER-CAMPUS TRANSIT" else "APPROVED • READY FOR GATE EXIT",
                            color = Color(0xFF059669),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isInter && destinationCampus.isNotBlank()) "Route: $campus ──► $destinationCampus" else "Allowed Departure: $departureTime • Local Exit",
                            color = Color(0xFF047857),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. HERO IDENTITY CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = Color.White,
                elevation = 2.dp,
                border = BorderStroke(1.dp, Color(0xFFEDF2F7))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Photo
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable { if (imageUrl.isNotEmpty()) onImageClick(imageUrl) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            color = Color(0xFF0F172A),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (uid.isNotBlank()) {
                            Text(
                                text = "UID: $uid",
                                color = ThemeLogoBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Text(
                            text = if (batch.isNotBlank()) "$department • Batch $batch" else department,
                            color = Color(0xFF64748B),
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }

        // 3. DEPARTURE & PASS REASON CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = Color.White,
                elevation = 1.dp,
                border = BorderStroke(1.dp, Color(0xFFEDF2F7))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Departure Details",
                        color = Color(0xFF0F172A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Reason
                    Text(
                        text = "PURPOSE OF EXIT",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = reason,
                        color = Color(0xFF1E293B),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Campus Route
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("FROM CAMPUS", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(campus, color = Color(0xFF1E293B), fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                        }
                        if (isInter && destinationCampus.isNotBlank()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("DESTINATION", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(destinationCampus, color = ThemeLogoBlue, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 4. TG / AUTHORITY APPROVAL REMARK
        if (tgRemark.isNotBlank() || role == "student") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = Color(0xFFF8FAFC),
                    elevation = 0.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ThemeLogoBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Teacher / Authority Clearance",
                                color = ThemeLogoBlue,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (tgRemark.isNotBlank()) "\"$tgRemark\"" else "Verified & Approved by Institutional Authority",
                            color = Color(0xFF334155),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontStyle = if (tgRemark.isNotBlank()) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                        )
                    }
                }
            }
        }

        // 5. EMERGENCY CONTACT & CALLING CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = Color.White,
                elevation = 1.dp,
                border = BorderStroke(1.dp, Color(0xFFEDF2F7))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Contact & Verification",
                        color = Color(0xFF0F172A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Candidate Phone
                    if (phone.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Candidate Phone", color = Color(0xFF64748B), fontSize = 11.5.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(phone, color = Color(0xFF0F172A), fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onCallPhone(phone) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFEFF6FF)),
                                elevation = ButtonDefaults.elevation(0.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = ThemeLogoBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Call", color = ThemeLogoBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Parent Phone
                    if (fatherPhone.isNotBlank() && role == "student") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(if (fatherName.isNotBlank()) "Parent ($fatherName)" else "Parent Phone", color = Color(0xFF64748B), fontSize = 11.5.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(fatherPhone, color = Color(0xFF0F172A), fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onCallPhone(fatherPhone) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFEFF6FF)),
                                elevation = ButtonDefaults.elevation(0.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = ThemeLogoBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Call Parent", color = ThemeLogoBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
