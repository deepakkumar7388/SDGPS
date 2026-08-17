package com.example.digitalpass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ThemeDarkBlue = Color(0xFF0A58CA)
val ThemeLightGreen = Color(0xFF20C997)
val ThemeGoldenOrange = Color(0xFFFD7E14)

@Composable
fun StatBox(modifier: Modifier = Modifier, value: String, label: String, brandColor: Color) {
    Card(
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color.White,
        elevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color(0xFFF0F4F8), RoundedCornerShape(12.dp))
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(6.dp)
                    .background(brandColor, CircleShape)
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = value,
                    color = Color(0xFF1C1F2E),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    color = Color(0xFF8E9297),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ActionListItem(title: String, icon: ImageVector, iconColor: Color, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = Color.White,
        elevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F4F8))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = Color(0xFF1C1F2E), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = Color(0xFFB0B3B8))
        }
    }
}

@Composable
fun SegmentedToggle(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color(0xFFF1F5F9), RoundedCornerShape(32.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(32.dp))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                val textColor = if (isSelected) Color.White else Color(0xFF64748B)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(ThemeLogoBlue, RoundedCornerShape(26.dp))
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SmallActionCard(modifier: Modifier, title: String, icon: ImageVector, onClick: () -> Unit = {}) {
    Card(
        modifier = modifier.height(80.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F4F8)),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = ThemeLogoBlue, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun LiveFeedItem(title: String, subtitle: String, time: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(Color(0xFFF0F4F8), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = ThemeLogoBlue, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        Text(time, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
fun AlertCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color(0xFFFFF4F4),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD6D6)),
        elevation = 0.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Warning, contentDescription = "Alert", tint = Color.Red)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Red)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, color = Color(0xFF555555), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun UserPhotoCard(
    name: String,
    status: String,
    imageUrl: String? = null,
    onClick: () -> Unit = {}
) {
    val statusLower = status.lowercase()
    val statusColor = when (statusLower) {
        "approved" -> Color(0xFF28A745)
        "approving" -> Color(0xFFF39C12)
        "rejected" -> Color(0xFFDC3545)
        "exit" -> Color(0xFF17A2B8)
        else -> Color(0xFF718096) // pending / default
    }

    val displayStatus = when (statusLower) {
        "approving" -> "In Process"
        else -> status.replaceFirstChar { it.lowercase() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        backgroundColor = Color(0xFFFBFBFE),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEDF2F7)),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image / Avatar badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = com.example.digitalpass.LoginUserDataHolder.getURL(imageUrl),
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ThemeLogoBlue.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.take(1).uppercase(),
                            color = ThemeLogoBlue,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Color(0xFF1C1F2E),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = displayStatus,
                    color = statusColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}