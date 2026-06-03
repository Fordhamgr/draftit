package com.flash.draft.ui.canvas

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft.model.CanvasElement

@Composable
fun PdfItem(
    pdfCard: CanvasElement.PdfCard, scale: Float, offset: Offset, isSelected: Boolean, isConnecting: Boolean, isConnectionTarget: Boolean = false, onConnectRequest: () -> Unit = {},
    onUpdate: (CanvasElement.PdfCard) -> Unit, onPositionDelta: (Offset) -> Unit, onSizeDelta: (Offset) -> Unit,
    onSaveRequest: () -> Unit, onDelete: () -> Unit, onSelect: () -> Unit
) {
    val context = LocalContext.current

    CanvasItemWrapper(
        position = pdfCard.position, width = pdfCard.width, height = pdfCard.height,
        scale = scale, canvasOffset = offset, isSelected = isSelected, isConnecting = isConnecting, isConnectionTarget = isConnectionTarget, onConnectRequest = onConnectRequest,
        isLocked = pdfCard.isLocked, onLockedChange = { onUpdate(pdfCard.copy(isLocked = it)) },
        backgroundAlpha = 1f,
        onSelect = onSelect, onPositionDelta = onPositionDelta, onSizeDelta = onSizeDelta,
        onDelete = onDelete, onSaveRequest = onSaveRequest,
        customMenuContent = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // Context menu toggle to view full PDF
                Row(
                    modifier = Modifier.clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(pdfCard.uriString), "application/pdf")
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No PDF app found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Launch, contentDescription = "Open File", tint = Color.Blue, modifier = Modifier.size(20.dp))
                    Text("Open PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Blue)
                }
            }
        }
    ) { isLocked ->
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.White.copy(alpha = 0.65f)), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(48.dp))
            }
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1B3D34).copy(alpha = 0.8f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(text = pdfCard.fileName, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "${pdfCard.fileSize} • PDF", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}