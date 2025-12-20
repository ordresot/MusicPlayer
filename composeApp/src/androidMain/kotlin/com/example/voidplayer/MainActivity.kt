package com.example.voidplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import com.example.voidplayer.data.AndroidSongRepository
import com.example.voidplayer.player.AndroidAudioPlayer

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = AndroidSongRepository(this)
        val audioPlayer = AndroidAudioPlayer(this)
        
        // State to verify folder selection - using Compose State to trigger updates in App
        val pickedFolderUri = mutableStateOf<String?>(null)
        val statusMsg = mutableStateOf("Ready")

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
           if(isGranted) recreate() 
        }
        
        // Folder Picker Launcher
        val folderPickerLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri != null) {
                statusMsg.value = "Selected: $uri"
                android.util.Log.d("VoidPlayer", "Folder picked: $uri")
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    statusMsg.value = "Permission Granted. Scaning..."
                    android.util.Log.d("VoidPlayer", "Persistable permission taken")
                    pickedFolderUri.value = uri.toString()
                } catch(e: Exception) { 
                    statusMsg.value = "Auth Error: ${e.message}"
                    android.util.Log.e("VoidPlayer", "Permission failed", e)
                }
            } else {
                statusMsg.value = "Cancelled by user"
                android.util.Log.d("VoidPlayer", "Folder picker cancelled")
            }
        }

        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        permissionLauncher.launch(permission)

        setContent {
            App(
                repository = repository,
                player = audioPlayer,
                pickedFolderUri = pickedFolderUri,
                statusMessage = statusMsg.value,
                onPickFolder = { 
                    statusMsg.value = "Launching Intent..."
                    android.widget.Toast.makeText(this, "Launching folder picker...", android.widget.Toast.LENGTH_SHORT).show()
                    folderPickerLauncher.launch(null)
                }
            )
        }
    }
}
