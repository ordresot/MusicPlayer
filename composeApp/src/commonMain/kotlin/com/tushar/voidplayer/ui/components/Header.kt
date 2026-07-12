package com.tushar.voidplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
            .padding(horizontal = 24.dp, vertical = 20.dp)
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
                    placeholder = { Text("Search...", color = SecondaryText) },
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
                            Icon(Icons.Filled.Close, null, tint = SecondaryText)
                        }
                    }
                )
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = PrimaryText
                    )
                    Text(
                        text = "My Music",
                        style = MaterialTheme.typography.titleSmall,
                        color = SecondaryText
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { isSearchActive = true }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = PrimaryText)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Text("âš™", fontSize = 24.sp, color = PrimaryText)
                    }
                    IconButton(onClick = onPickFolder) {
                        Text("ðŸ“", fontSize = 24.sp, color = PrimaryText)
                    }
                }
            }
        }
    }
}
