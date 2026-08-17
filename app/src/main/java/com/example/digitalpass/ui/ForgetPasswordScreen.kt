package com.example.digitalpass.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ResetPasswordStep {
    ENTER_EMAIL,
    VERIFY_OTP,
    RESET_PASSWORD
}

@Composable
fun ForgetPasswordScreen(
    currentStep: ResetPasswordStep,
    emailValue: String,
    countdownText: String,
    canResend: Boolean,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSendCode: (email: String) -> Unit,
    onVerifyCode: (code: String) -> Unit,
    onResetPassword: (newPass: String, confirmPass: String) -> Unit,
    onResendClick: () -> Unit
) {
    var emailInput by remember(emailValue) { mutableStateOf(emailValue) }
    var codeInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

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
                            text = "Reset Password",
                            color = Color(0xFF0F172A),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = when (currentStep) {
                                ResetPasswordStep.ENTER_EMAIL -> "Step 1 of 3: Enter Email"
                                ResetPasswordStep.VERIFY_OTP -> "Step 2 of 3: Verify OTP"
                                ResetPasswordStep.RESET_PASSWORD -> "Step 3 of 3: New Password"
                            },
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Recovery Hero Icon
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(2.dp, ThemeLogoBlue.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (currentStep) {
                                ResetPasswordStep.ENTER_EMAIL -> Icons.Outlined.Email
                                ResetPasswordStep.VERIFY_OTP -> Icons.Default.CheckCircle
                                ResetPasswordStep.RESET_PASSWORD -> Icons.Outlined.Lock
                            },
                            contentDescription = null,
                            tint = ThemeLogoBlue,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (currentStep) {
                        ResetPasswordStep.ENTER_EMAIL -> "Forgot Password?"
                        ResetPasswordStep.VERIFY_OTP -> "Verify Code"
                        ResetPasswordStep.RESET_PASSWORD -> "Create New Password"
                    },
                    color = Color(0xFF0F172A),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (currentStep) {
                        ResetPasswordStep.ENTER_EMAIL -> "Enter your registered email address to receive a recovery verification code."
                        ResetPasswordStep.VERIFY_OTP -> "We've sent a 6-digit code to $emailValue."
                        ResetPasswordStep.RESET_PASSWORD -> "Set a strong new password for your account."
                    },
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Card Container
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = Color.White,
                    elevation = 3.dp,
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        when (currentStep) {
                            ResetPasswordStep.ENTER_EMAIL -> {
                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("Registered Email Address", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = ThemeLogoBlue) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = ThemeLogoBlue,
                                        focusedLabelColor = ThemeLogoBlue,
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    )
                                )

                                Spacer(modifier = Modifier.height(22.dp))

                                Button(
                                    onClick = { onSendCode(emailInput) },
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = ThemeLogoBlue),
                                    elevation = ButtonDefaults.elevation(4.dp, 8.dp)
                                ) {
                                    Text("Send Verification Code", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            ResetPasswordStep.VERIFY_OTP -> {
                                OutlinedTextField(
                                    value = codeInput,
                                    onValueChange = { codeInput = it },
                                    label = { Text("Verification Code", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ThemeLogoBlue) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), letterSpacing = 2.sp),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = ThemeLogoBlue,
                                        focusedLabelColor = ThemeLogoBlue,
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = countdownText,
                                        color = if (canResend) ThemeLogoBlue else Color(0xFF64748B),
                                        fontSize = 13.sp,
                                        fontWeight = if (canResend) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.clickable(enabled = canResend) { onResendClick() }
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = { onVerifyCode(codeInput) },
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = ThemeLogoBlue),
                                    elevation = ButtonDefaults.elevation(4.dp, 8.dp)
                                ) {
                                    Text("Verify & Continue", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            ResetPasswordStep.RESET_PASSWORD -> {
                                OutlinedTextField(
                                    value = newPasswordInput,
                                    onValueChange = { newPasswordInput = it },
                                    label = { Text("New Password", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = ThemeLogoBlue) },
                                    trailingIcon = {
                                        TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Text(if (isPasswordVisible) "HIDE" else "SHOW", color = ThemeLogoBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = ThemeLogoBlue,
                                        focusedLabelColor = ThemeLogoBlue,
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = confirmPasswordInput,
                                    onValueChange = { confirmPasswordInput = it },
                                    label = { Text("Confirm New Password", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = ThemeLogoBlue) },
                                    trailingIcon = {
                                        TextButton(onClick = { isConfirmVisible = !isConfirmVisible }) {
                                            Text(if (isConfirmVisible) "HIDE" else "SHOW", color = ThemeLogoBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    visualTransformation = if (isConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = ThemeLogoBlue,
                                        focusedLabelColor = ThemeLogoBlue,
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    )
                                )

                                Spacer(modifier = Modifier.height(22.dp))

                                Button(
                                    onClick = { onResetPassword(newPasswordInput, confirmPasswordInput) },
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = ThemeLogoBlue),
                                    elevation = ButtonDefaults.elevation(4.dp, 8.dp)
                                ) {
                                    Text("Update Password", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
}
