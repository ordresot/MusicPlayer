package com.example.voidplayer.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.voidplayer.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidSongRepository(private val context: Context) : SongRepository {
    override suspend fun getSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = null // Get EVERYTHING
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        android.util.Log.d("VoidPlayer", "Querying songs from $uri")

        try {
            context.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                android.util.Log.d("VoidPlayer", "Found ${cursor.count} songs")
                
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown"
                    val artist = cursor.getString(artistColumn) ?: "Unknown"
                    val duration = cursor.getLong(durationColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()
                    
                    if (duration > 10000) { // Filter manually in loop
                         songs.add(Song(id, title, artist, duration, contentUri))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("VoidPlayer", "Error querying songs", e)
        }
        android.util.Log.d("VoidPlayer", "Returning ${songs.size} valid songs")
        songs
    }

    override suspend fun loadFromFolder(uriString: String): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        try {
            val treeUri = android.net.Uri.parse(uriString)
            val docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
            
            if (docFile != null && docFile.isDirectory) {
                traverseDirectory(docFile, songs)
            }
        } catch (e: Exception) {
            android.util.Log.e("VoidPlayer", "Error loading from folder", e)
        }
        songs
    }

    private fun traverseDirectory(dir: androidx.documentfile.provider.DocumentFile, songs: MutableList<Song>) {
        val files = dir.listFiles()
        android.util.Log.d("VoidPlayer", "Traversing ${dir.name}: found ${files.size} files")
        
        for (file in files) {
            if (file.isDirectory) {
                traverseDirectory(file, songs)
            } else {
                if (isValidAudioFile(file.name)) {
                   android.util.Log.d("VoidPlayer", "Found audio: ${file.name}")
                   songs.add(
                       Song(
                           id = System.currentTimeMillis() + songs.size, // Pseudo ID
                           title = file.name ?: "Unknown",
                           artist = "Unknown Artist",
                           duration = 0L, // Need MetadataRetriever for this, skipping for speed or fallback
                           uri = file.uri.toString()
                       )
                   )
                }
            }
        }
    }

    private fun isValidAudioFile(name: String?): Boolean {
        if (name == null) return false
        val lower = name.lowercase()
        return lower.endsWith(".mp3") || 
               lower.endsWith(".wav") || 
               lower.endsWith(".flac") || 
               lower.endsWith(".aac") || 
               lower.endsWith(".ogg") || 
               lower.endsWith(".m4a") || 
               lower.endsWith(".wma") ||
               lower.endsWith(".opus") ||
               lower.endsWith(".amr")
    }
}
