package com.flash.draft.ui.home

import androidx.compose.ui.res.painterResource
import com.composables.icons.lucide.R as LucideR
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft.data.NoteRepository
import com.flash.draft.model.Project
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun DashboardScreen(
    repository: NoteRepository,
    onProjectClick: (String) -> Unit,
    onCreateNewProject: () -> Unit
) {
    // We use a mutable state list so the UI updates if we delete a project
    var projects by remember { mutableStateOf(repository.getAllProjects()) }
    val isDark = false // You can tie this to system theme later

    val bgColor = if (isDark) Color(0xFF121212) else Color(0xFFF5F5F7)
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1F)
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonContent = reader.use { it.readText() }
                val newProjectId = repository.importProject(jsonContent)
                if (newProjectId != null) {
                    projects = repository.getAllProjects()
                    Toast.makeText(context, "Project imported successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Invalid project file format.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

            // Premium Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All canvas",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                
                IconButton(
                    onClick = { /* TODO Help */ },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = if (isDark) 0.1f else 0.8f), RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Text("?", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = textColor)
                }
            }

            if (projects.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No boards yet. Create one to get started.", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(projects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            isDark = isDark,
                            onClick = { onProjectClick(project.id) },
                            onDelete = {
                                repository.deleteProject(project.id)
                                projects = repository.getAllProjects() // Refresh list
                            }
                        )
                    }
                }
            }
        }

        // Floating Actions at the bottom (Pill)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.2f))
                .background(Color.White.copy(alpha = if (isDark) 0.15f else 0.85f), RoundedCornerShape(32.dp))
                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(32.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Filled.FileDownload, contentDescription = "Import", modifier = Modifier.size(20.dp), tint = textColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import", fontWeight = FontWeight.Medium, color = textColor)
            }

            Button(
                onClick = onCreateNewProject,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1), contentColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(painterResource(id = LucideR.drawable.lucide_ic_plus), contentDescription = "New Project", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Project", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    isDark: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBg = Color.White.copy(alpha = if (isDark) 0.1f else 0.7f)
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1F)
    val dateColor = if (isDark) Color.Gray else Color(0xFF86868B)

    val formatter = SimpleDateFormat("MMM d, yyyy", LocalLocale.current.platformLocale)
    val dateString = formatter.format(Date(project.lastModifiedMs))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.1f))
            .background(cardBg, RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = if (isDark) 0.1f else 0.6f), RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        // Thumbnail Area (Mocking a preview)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color.White.copy(alpha = if (isDark) 0.05f else 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            // Future feature: Actually draw miniaturized elements here based on project.previewElements
            Text("${project.previewElements.size} items", color = dateColor, fontSize = 12.sp)
        }

        // Card Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    maxLines = 1
                )
                Text(
                    text = dateString,
                    fontSize = 12.sp,
                    color = dateColor
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(painterResource(id = LucideR.drawable.lucide_ic_trash), contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}