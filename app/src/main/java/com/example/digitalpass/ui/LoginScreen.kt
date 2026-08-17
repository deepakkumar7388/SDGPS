package com.example.digitalpass.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitalpass.R

@Composable
fun LoginScreen(
    initialEmail: String = "",
    initialPassword: String = "",
    isLoading: Boolean,
    onLoginClick: (email: String, pass: String) -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    var emailText by remember(initialEmail) { mutableStateOf(initialEmail) }
    var passwordText by remember(initialPassword) { mutableStateOf(initialPassword) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 1. Top 3D Header Illustration (Tall & Immersive)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.login_header_bg),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // 2. Overlapping White Login Card (Spacious & Prominent)
            Column(
                modifier = Modifier
                    .offset(y = (-55).dp)
                    .padding(horizontal = 22.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(36.dp),
                    backgroundColor = Color.White,
                    elevation = 10.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Logo + DIGITAL PASS Brand Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logofornotification2),
                                contentDescription = "Digital Pass Logo",
                                modifier = Modifier.size(58.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "DIGITAL",
                                    color = Color(0xFF4B72FA),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "PASS",
                                    color = Color(0xFF7E57FF),
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Seamless Access. Anytime. Anywhere.",
                            color = Color(0xFF8F9CAE),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Username / Email Input Field
                        OutlinedTextField(
                            value = emailText,
                            onValueChange = { emailText = it },
                            label = {
                                Text(
                                    text = "Enter Username",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = null,
                                    tint = Color(0xFF8F9CAE),
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp,
                                color = Color(0xFF1C1F2E),
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF94A3B8),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedLabelColor = Color(0xFF475569),
                                unfocusedLabelColor = Color(0xFF8F9CAE),
                                backgroundColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password Input Field
                        OutlinedTextField(
                            value = passwordText,
                            onValueChange = { passwordText = it },
                            label = {
                                Text(
                                    text = "Password",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF8F9CAE),
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { isPasswordVisible = !isPasswordVisible },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    EyeToggleIcon(isVisible = isPasswordVisible, tint = Color(0xFF8F9CAE))
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onLoginClick(emailText, passwordText)
                                }
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp,
                                color = Color(0xFF1C1F2E),
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF94A3B8),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedLabelColor = Color(0xFF475569),
                                unfocusedLabelColor = Color(0xFF8F9CAE),
                                backgroundColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Right Aligned "Forgot Password?"
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "Forgot Password?",
                                color = Color(0xFF4B72FA),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onForgotPasswordClick() }
                                    .padding(vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Gradient "Login" Button (Spacious & Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF4B72FA),
                                            Color(0xFF7E57FF)
                                        )
                                    )
                                )
                                .clickable(enabled = !isLoading) {
                                    focusManager.clearFocus()
                                    onLoginClick(emailText, passwordText)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
                            } else {
                                Text(
                                    text = "Login",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer Text
                Text(
                    text = "If you have no account, please contact your administrator for registration.",
                    color = Color(0xFF757575),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun EyeToggleIcon(isVisible: Boolean, tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height

        // Outer Eye Curve
        val path = Path().apply {
            moveTo(2f, h / 2f)
            quadraticBezierTo(w / 2f, 2f, w - 2f, h / 2f)
            quadraticBezierTo(w / 2f, h - 2f, 2f, h / 2f)
            close()
        }
        drawPath(path = path, color = tint, style = Stroke(width = 2.2f.dp.toPx()))

        // Pupil
        drawCircle(
            color = tint,
            radius = if (isVisible) 3.5f.dp.toPx() else 4.5f.dp.toPx(),
            center = Offset(w / 2f, h / 2f)
        )

        // Strikethrough Slash if Hidden
        if (!isVisible) {
            drawLine(
                color = tint,
                start = Offset(4f, h - 4f),
                end = Offset(w - 4f, 4f),
                strokeWidth = 2.2f.dp.toPx()
            )
        }
    }
}
