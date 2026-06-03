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
            Text(
                text = "All Boards",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(bottom = 24.dp)
            )

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

        // Floating Action Button (Glassmorphism inspired)
        FloatingActionButton(
            onClick = onCreateNewProject,
            containerColor = Color(0xFF2196F3),
            contentColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
                .size(64.dp)
                .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFF2196F3))
        ) {
            Icon(painterResource(id = LucideR.drawable.lucide_ic_plus), contentDescription = "New Board", modifier = Modifier.size(32.dp))
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
    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1F)
    val dateColor = if (isDark) Color.Gray else Color(0xFF86868B)

    val formatter = SimpleDateFormat("MMM d, yyyy", LocalLocale.current.platformLocale)
    val dateString = formatter.format(Date(project.lastModifiedMs))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(cardBg, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        // Thumbnail Area (Mocking a preview)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color(0xFFE5E5EA).copy(alpha = if (isDark) 0.1f else 0.5f)),
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