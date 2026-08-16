package com.tushar.voidplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushar.voidplayer.ui.theme.PrimaryText
import com.tushar.voidplayer.ui.theme.SecondaryText
import com.tushar.voidplayer.ui.theme.SurfaceElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header(
    onPickFolder: () -> Unit,
    onOpenSettings: () -> Unit,
    searchQuery: String,
    onSearchCb: (String) -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSearchActive) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchCb,
                    placeholder = { Text("Search title or artist...", color = SecondaryText) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .background(SurfaceElevated, RoundedCornerShape(16.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            onSearchCb("")
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close search", tint = SecondaryText)
                        }
                    }
                )
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Void Player",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = PrimaryText
                    )
                    Text(
                        text = "Hi-Fi Audio Experience",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { isSearchActive = true }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = PrimaryText)
                    }
                    IconButton(onClick = onPickFolder) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Select Music Folder", tint = PrimaryText)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Audio Settings", tint = PrimaryText)
                    }
                }
            }
        }
    }
}
