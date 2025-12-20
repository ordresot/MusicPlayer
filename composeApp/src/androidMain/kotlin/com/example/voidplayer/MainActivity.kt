package com.example.voidplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.voidplayer.data.AndroidSongRepository
import com.example.voidplayer.player.AndroidAudioPlayer

class MainActivity : ComponentActivity() {
    
    // Simple state to force recomposition if needed
    // In a real app, use a ViewModel.
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
           // Recreate or refresh if needed. 
           // For now, valid repository usage handles "no permission" gracefully (returns empty).
           // If granted, we ideally want to refresh the list.
           if(isGranted) {
               // A simple way to refresh is to recreate the activity or use a ViewModel state.
               // For this straightforward implementation, we'll just let the user restart or hope App.kt re-triggers.
               // Actually App.kt creates repository once.
               recreate() 
           }
        }

        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        permissionLauncher.launch(permission)

        val repository = AndroidSongRepository(this)
        val audioPlayer = AndroidAudioPlayer(this)

        setContent {
            App(repository, audioPlayer)
        }
    }
}
