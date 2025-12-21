package com.example.voidplayer

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val app = VoidPlayerApp.instance
        val sharedPref = getSharedPreferences("VoidPlayerPrefs", android.content.Context.MODE_PRIVATE)
        val savedUri = sharedPref.getString("last_folder_uri", null)
        
        val pickedFolderUri = mutableStateOf(savedUri)
        val statusMsg = mutableStateOf(if (savedUri != null) "Restored Folder" else "Ready")

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
           if(!isGranted) {
               statusMsg.value = "Permission Denied"
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
                    sharedPref.edit().putString("last_folder_uri", uri.toString()).apply()
                    statusMsg.value = "Folder Saved"
                } catch(e: Exception) { 
                    statusMsg.value = "Folder Auth Failed"
                }
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        permissionLauncher.launch(permission)

        setContent {
            App(
                repository = app.repository,
                player = app.player,
                pickedFolderUri = pickedFolderUri,
                statusMessage = statusMsg.value,
                onPickFolder = { 
                    folderPickerLauncher.launch(null)
                }
            )
        }
    }

    override fun onStop() {
        super.onStop()
        val intent = Intent(this, com.example.voidplayer.service.OverlayService::class.java)
        startService(intent)
    }

    override fun onStart() {
        super.onStart()
        android.util.Log.d("VoidPlayer", "MainActivity onStart - Stopping OverlayService")
        val intent = Intent(this, com.example.voidplayer.service.OverlayService::class.java)
        stopService(intent)
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("VoidPlayer", "MainActivity onResume - Stopping OverlayService")
        val intent = Intent(this, com.example.voidplayer.service.OverlayService::class.java)
        stopService(intent)
    }
}
