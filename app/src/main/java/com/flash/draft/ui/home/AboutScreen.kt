package com.flash.draft.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDark = false
    val bgColor = if (isDark) Color(0xFF121212) else Color(0xFFF5F5F7)
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1F)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = if (isDark) 0.1f else 0.8f), RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                }
                
                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "About DraftIt",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            // Content Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.1f))
                    .background(Color.White.copy(alpha = if (isDark) 0.1f else 0.7f), RoundedCornerShape(32.dp))
                    .border(1.dp, Color.White.copy(alpha = if (isDark) 0.1f else 0.6f), RoundedCornerShape(32.dp))
                    .padding(32.dp)
            ) {
                Text(
                    text = "Version 1.0.0",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "DraftIt is an infinite canvas built entirely in Jetpack Compose. It's designed to be blazingly fast, buttery smooth, and completely boundless. Use it to brainstorm, mind map, design systems, or just doodle your ideas out.",
                    fontSize = 16.sp,
                    color = textColor.copy(alpha = 0.8f),
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                HorizontalDivider(color = textColor.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Performance Metrics",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• 120 FPS Target\n• Zero-allocation panning\n• Hardware-accelerated blurs\n• Atomic data sync",
                    fontSize = 16.sp,
                    color = textColor.copy(alpha = 0.8f),
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { android.widget.Toast.makeText(context, "You are up to date! (Coming Soon)", android.widget.Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("Check for Updates", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Legal Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { android.widget.Toast.makeText(context, "Privacy Policy Coming Soon", android.widget.Toast.LENGTH_SHORT).show() }) {
                        Text("Privacy Policy", color = textColor.copy(alpha = 0.6f))
                    }
                    TextButton(onClick = { android.widget.Toast.makeText(context, "Terms of Service Coming Soon", android.widget.Toast.LENGTH_SHORT).show() }) {
                        Text("Terms of Service", color = textColor.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}
