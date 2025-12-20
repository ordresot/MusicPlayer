package com.example.voidplayer.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import com.example.voidplayer.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidSongRepository(private val context: Context) : SongRepository {
    
    private val specificFolder = "Music" // Default specific folder name or part of path

    override suspend fun getSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        // Filter for specific folder in MediaStore if possible, or just get all and filter
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                val retriever = MediaMetadataRetriever()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val duration = cursor.getLong(durationColumn)

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    
                    if (duration > 5000) {
                        var art: ByteArray? = null
                        try {
                            retriever.setDataSource(context, contentUri)
                            art = retriever.embeddedPicture
                        } catch (e: Exception) { /* ignore */ }
                        
                        songs.add(Song(id, title, artist, duration, contentUri.toString(), art))
                    }
                }
                retriever.release()
            }
        } catch (e: Exception) {
            android.util.Log.e("VoidPlayer", "Error querying MediaStore", e)
        }
        songs
    }

    override suspend fun loadFromFolder(uriString: String): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val retriever = MediaMetadataRetriever()
        try {
            val treeUri = android.net.Uri.parse(uriString)
            val docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
            
            if (docFile != null && docFile.isDirectory) {
                traverseDirectory(docFile, songs, retriever)
            }
        } catch (e: Exception) {
            android.util.Log.e("VoidPlayer", "Error loading from folder: $uriString", e)
        } finally {
            retriever.release()
        }
        songs
    }

    private fun traverseDirectory(
        dir: androidx.documentfile.provider.DocumentFile, 
        songs: MutableList<Song>,
        retriever: MediaMetadataRetriever
    ) {
        val files = dir.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                traverseDirectory(file, songs, retriever)
            } else if (isValidAudioFile(file.name)) {
                try {
                    retriever.setDataSource(context, file.uri)
                    val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.name ?: "Unknown"
                    val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                    val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                    val art = retriever.embeddedPicture
                    
                    songs.add(
                        Song(
                            id = file.uri.hashCode().toLong(),
                            title = title,
                            artist = artist,
                            duration = duration,
                            uri = file.uri.toString(),
                            coverArt = art
                        )
                    )
                } catch (e: Exception) {
                    songs.add(Song(file.uri.hashCode().toLong(), file.name ?: "Unknown", "Unknown", 0L, file.uri.toString()))
                }
            }
        }
    }

    private fun isValidAudioFile(name: String?): Boolean {
        val extensions = listOf(".mp3", ".wav", ".flac", ".aac", ".ogg", ".m4a", ".opus")
        return extensions.any { name?.lowercase()?.endsWith(it) == true }
    }
}
