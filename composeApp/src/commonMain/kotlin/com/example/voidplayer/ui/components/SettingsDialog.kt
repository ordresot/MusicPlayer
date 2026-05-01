package com.example.voidplayer.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voidplayer.player.AudioPlayer
import com.example.voidplayer.ui.theme.PrimaryText
import com.example.voidplayer.ui.theme.SurfaceElevated
import com.example.voidplayer.ui.theme.SurfaceVariant

@Composable
fun SettingsDialog(onDismiss: () -> Unit, player: AudioPlayer, accentColor: Color) {
    var showEqualizer by remember { mutableStateOf(false) }

    if (showEqualizer) {
        EqualizerDialog(onDismiss = { showEqualizer = false }, player = player, accentColor = accentColor)
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = SurfaceElevated,
            title = {
                Text("SETTINGS", color = accentColor, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { showEqualizer = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("EQUALIZER", color = PrimaryText)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Audio Normalization", color = PrimaryText, fontSize = 14.sp)
                        Switch(
                            checked = true,
                            onCheckedChange = { },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha=0.5f))
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("CLOSE", color = accentColor)
                }
            }
        )
    }
}

@Composable
fun EqualizerDialog(onDismiss: () -> Unit, player: AudioPlayer, accentColor: Color) {
    val bands by player.equalizerBands.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Text("EQUALIZER", color = accentColor, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                verticalArrangement = Arrangement.Center
            ) {
                if (bands.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Start music to use equalizer", color = Color.Gray)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        bands.forEachIndexed { index, band ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(60.dp)
                            ) {
                                Text(
                                    text = "${band.level / 100}dB",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = band.level.toFloat(),
                                    onValueChange = { player.setEqualizerBandLevel(index, it.toInt()) },
                                    valueRange = band.minLevel.toFloat()..band.maxLevel.toFloat(),
                                    modifier = Modifier.height(180.dp).graphicsLayer {
                                        rotationZ = -90f
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = accentColor,
                                        activeTrackColor = accentColor,
                                        inactiveTrackColor = Color.DarkGray
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (band.frequency >= 1000) "${band.frequency / 1000}k" else "${band.frequency}",
                                    color = PrimaryText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("DONE", color = accentColor)
            }
        }
    )
}
