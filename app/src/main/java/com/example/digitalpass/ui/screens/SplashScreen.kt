package com.example.digitalpass.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitalpass.*
import com.example.digitalpass.ui.theme.*
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: (role: String) -> Unit
) {
    val context = LocalContext.current

    // ── Animations ──
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")

    // Logo entrance animation
    var animationStarted by remember { mutableStateOf(false) }
    val logoAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
        label = "logoAlpha"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logoScale"
    )
    val logoRotation by animateFloatAsState(
        targetValue = if (animationStarted) 0f else -90f,
        animationSpec = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
        label = "logoRotation"
    )

    // App name entrance
    val textAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 800, easing = FastOutSlowInEasing),
        label = "textAlpha"
    )
    val textScale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "textScale"
    )

    // Breathing effect (continuous)
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // Background color transition
    val bgProgress by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
        label = "bgProgress"
    )

    LaunchedEffect(Unit) {
        animationStarted = true

        // Wait for animation to be appreciated, then check session
        delay(3200)
        checkSession(context, onNavigateToLogin, onNavigateToDashboard)
    }

    // Interpolate background color
    val bgColor = lerp(BrandPrimary, SurfaceWhite, bgProgress)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Logo ──
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(logoScale * breatheScale)
                    .alpha(logoAlpha)
                    .rotate(logoRotation),
                contentAlignment = Alignment.Center
            ) {
                // Use the text "DP" as a placeholder since we can't load PNG in pure Compose preview
                // In production, this would use an Image composable with the app logo
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(GradientStart, GradientEnd)
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DP",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── App Name ──
            Text(
                text = "Digital Pass",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .alpha(textAlpha)
                    .scale(textScale)
            )
        }
    }
}

private fun checkSession(
    context: Context,
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: (role: String) -> Unit
) {
    val sharedPreferences = context.getSharedPreferences("DigitalPassPrefs", Context.MODE_PRIVATE)
    val token = sharedPreferences.getString("token", null)

    if (token != null) {
        val loginWithToken = RetrofitClient.instance.loginUser(LoginData("", token))
        loginWithToken.enqueue(object : Callback<HashMap<String, String>> {
            override fun onResponse(
                call: Call<HashMap<String, String>>,
                response: Response<HashMap<String, String>>
            ) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    LoginUserDataHolder.loginUserData = responseBody
                    LoginUserDataHolder.token = token
                    LoginUserDataHolder.saveState(context)
                    val role = responseBody?.get("role")
                    if (role != null) {
                        onNavigateToDashboard(role)
                    } else {
                        onNavigateToLogin()
                    }
                } else {
                    onNavigateToLogin()
                }
            }

            override fun onFailure(call: Call<HashMap<String, String>>, t: Throwable) {
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                onNavigateToLogin()
            }
        })
    } else {
        onNavigateToLogin()
    }
}

// Simple color lerp utility
private fun lerp(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
}
