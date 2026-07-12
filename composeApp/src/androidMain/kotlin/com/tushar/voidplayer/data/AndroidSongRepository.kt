package com.tushar.voidplayer.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import com.tushar.voidplayer.model.Song
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

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val duration = cursor.getLong(durationColumn)

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    
                    // Allow all audio files, even if duration is not yet reported (e.g. FLACs)
                    if (duration >= 0) {
                        // Art is loaded lazily via ImageCache to drastically speed up initial load
                        songs.add(Song(id, title, artist, duration, contentUri.toString(), null))
                    }
                }
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

    override suspend fun loadArt(uriString: String): ByteArray? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, android.net.Uri.parse(uriString))
            val art = retriever.embeddedPicture
            retriever.release()
            art
        } catch (e: Throwable) {
            // Catches Exception, Error (e.g. OutOfMemoryError on large embedded art),
            // and any other throwable so that art loading never crashes the app.
            android.util.Log.e("VoidPlayer", "Failed to load art for $uriString", e)
            try { retriever.release() } catch (_: Throwable) {}
            null
        }
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
                var title = file.name ?: "Unknown"
                var artist = "Unknown Artist" 
                var duration = 0L
                
                try {
                    retriever.setDataSource(context, file.uri)
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: title
                    artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: artist
                    duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                } catch (e: Exception) {
                    // Ignore, fallback to defaults
                }
                
                songs.add(
                    Song(
                        id = file.uri.hashCode().toLong(),
                        title = title,
                        artist = artist,
                        duration = duration,
                        uri = file.uri.toString(),
                        coverArt = null
                    )
                )
            }
        }
    }

    private fun isValidAudioFile(name: String?): Boolean {
        val extensions = listOf(".mp3", ".wav", ".flac", ".aac", ".ogg", ".m4a", ".opus")
        return extensions.any { name?.lowercase()?.endsWith(it) == true }
    }
}
