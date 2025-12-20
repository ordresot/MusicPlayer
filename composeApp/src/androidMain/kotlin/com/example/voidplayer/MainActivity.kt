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
    
    private lateinit var audioPlayer: AndroidAudioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = AndroidSongRepository(this)
        audioPlayer = AndroidAudioPlayer(this)
        
        val pickedFolderUri = mutableStateOf<String?>(null)
        val statusMsg = mutableStateOf("Ready")

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
           if(!isGranted) {
               statusMsg.value = "Permission Denied. Library empty."
               android.util.Log.w("VoidPlayer", "Storage permission denied")
           }
        }
        
        val folderPickerLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    pickedFolderUri.value = uri.toString()
                } catch(e: Exception) { 
                    statusMsg.value = "Folder Auth Failed"
                    android.util.Log.e("VoidPlayer", "Persistable permission failed", e)
                }
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
                    folderPickerLauncher.launch(null)
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::audioPlayer.isInitialized) {
            audioPlayer.cleanUp()
        }
    }
}
