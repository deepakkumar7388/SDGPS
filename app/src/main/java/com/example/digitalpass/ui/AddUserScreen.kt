package com.example.digitalpass.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitalpass.LoginUserDataHolder

@Composable
fun AddUserScreen(
    departments: List<String>,
    roles: List<String>,
    batches: List<String>,
    isLoading: Boolean,
    onDepartmentSelected: (String) -> Unit,
    onRoleSelected: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: (formData: Map<String, String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedDepartment by remember(departments) { mutableStateOf(departments.firstOrNull() ?: "") }
    var selectedRole by remember(roles) { mutableStateOf(roles.firstOrNull() ?: "") }
    var selectedBatch by remember(batches) { mutableStateOf(batches.firstOrNull() ?: "") }
    var uid by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var fatherPhone by remember { mutableStateOf("") }

    val isStudent = selectedRole.equals("student", ignoreCase = true)

    val isFormValid = name.isNotBlank() && email.isNotBlank() && phone.isNotBlank() &&
            selectedDepartment.isNotBlank() && selectedRole.isNotBlank() &&
            (!isStudent || (uid.isNotBlank() && fatherName.isNotBlank() && fatherPhone.isNotBlank() && selectedBatch.isNotBlank()))

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
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    Column {
                        Text(
                            text = "Add New User",
                            color = Color(0xFF0F172A),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        },
        bottomBar = {
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
                    Button(
                        onClick = {
                            if (isFormValid) {
                                val map = mutableMapOf(
                                    "name" to name.trim(),
                                    "email" to email.trim(),
                                    "phone" to phone.trim(),
                                    "department" to selectedDepartment.trim(),
                                    "role" to selectedRole.trim()
                                )
                                if (isStudent) {
                                    map["uid"] = uid.trim()
                                    map["fathername"] = fatherName.trim()
                                    map["fatherphone"] = fatherPhone.trim()
                                    map["batch"] = selectedBatch.trim()
                                }
                                onSubmit(map)
                            }
                        },
                        enabled = isFormValid && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = ThemeLogoBlue,
                            disabledBackgroundColor = Color(0xFFCBD5E1)
                        ),
                        elevation = ButtonDefaults.elevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Add User",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION 1: ROLE & DEPARTMENT ASSIGNMENT
                item {
                    SectionCard(
                        step = "1",
                        title = "Department & Designation"
                    ) {
                        ModernDropdownField(
                            label = "Department",
                            icon = Icons.Outlined.AccountBox,
                            options = departments,
                            selectedOption = selectedDepartment,
                            onOptionSelected = {
                                selectedDepartment = it
                                onDepartmentSelected(it)
                            }
                        )

                        ModernDropdownField(
                            label = "Role",
                            icon = Icons.Outlined.Person,
                            options = roles,
                            selectedOption = selectedRole,
                            onOptionSelected = {
                                selectedRole = it
                                onRoleSelected(it)
                            }
                        )
                    }
                }

                // SECTION 2: PERSONAL DETAILS
                item {
                    SectionCard(
                        step = "2",
                        title = "Personal Details"
                    ) {
                        ModernInputField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Full Name",
                            icon = Icons.Outlined.Person
                        )

                        ModernInputField(
                            value = email,
                            onValueChange = { email = it },
                            label = "Email Address",
                            icon = Icons.Outlined.Email,
                            keyboardType = KeyboardType.Email
                        )

                        ModernInputField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = "Phone Number",
                            icon = Icons.Outlined.Phone,
                            keyboardType = KeyboardType.Phone
                        )
                    }
                }

                // SECTION 3: STUDENT SPECIFIC FIELDS (Animated)
                if (isStudent) {
                    item {
                        SectionCard(
                            step = "3",
                            title = "Academic & Guardian Info",
                            accentColor = Color(0xFF059669)
                        ) {
                            ModernDropdownField(
                                label = "Batch",
                                icon = Icons.Outlined.DateRange,
                                options = batches,
                                selectedOption = selectedBatch,
                                onOptionSelected = { selectedBatch = it }
                            )

                            ModernInputField(
                                value = uid,
                                onValueChange = { uid = it },
                                label = "Enrollment Number / UID",
                                icon = Icons.Outlined.Info
                            )

                            ModernInputField(
                                value = fatherName,
                                onValueChange = { fatherName = it },
                                label = "Father's Name",
                                icon = Icons.Outlined.Person
                            )

                            ModernInputField(
                                value = fatherPhone,
                                onValueChange = { fatherPhone = it },
                                label = "Father's Phone Number",
                                icon = Icons.Outlined.Phone,
                                keyboardType = KeyboardType.Phone
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    step: String,
    title: String,
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
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
private fun ModernInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0F172A)
        ),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            backgroundColor = Color.White,
            focusedBorderColor = ThemeLogoBlue,
            unfocusedBorderColor = Color(0xFFCBD5E1),
            focusedLabelColor = ThemeLogoBlue,
            unfocusedLabelColor = Color(0xFF64748B),
            cursorColor = ThemeLogoBlue
        )
    )
}

@Composable
private fun ModernDropdownField(
    label: String,
    icon: ImageVector,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
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
                    tint = if (selectedOption.isNotBlank()) ThemeLogoBlue else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                IconButton(onClick = { if (options.isNotEmpty()) expanded = !expanded }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = if (selectedOption.isNotBlank()) ThemeLogoBlue else Color(0xFF64748B)
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (options.isNotEmpty()) expanded = !expanded
                },
            enabled = false, // Prevents keyboard while allowing click on surface
            shape = RoundedCornerShape(14.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0F172A)
            ),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                disabledTextColor = Color(0xFF0F172A),
                disabledBorderColor = if (selectedOption.isNotBlank()) ThemeLogoBlue.copy(alpha = 0.6f) else Color(0xFFCBD5E1),
                disabledLabelColor = if (selectedOption.isNotBlank()) ThemeLogoBlue else Color(0xFF64748B),
                disabledLeadingIconColor = if (selectedOption.isNotBlank()) ThemeLogoBlue else Color(0xFF94A3B8),
                disabledTrailingIconColor = Color(0xFF64748B),
                backgroundColor = Color.White
            )
        )

        // Overlay transparent clickable box to open dropdown
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { if (options.isNotEmpty()) expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(Color.White)
        ) {
            options.filter { it.isNotBlank() && it != "Select..." && it != "Select Role" }.forEach { option ->
                val isSelected = (option == selectedOption)
                DropdownMenuItem(
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            fontSize = 14.5.sp,
                            color = if (isSelected) ThemeLogoBlue else Color(0xFF1E293B),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = ThemeLogoBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
