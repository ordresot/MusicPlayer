package com.tushar.voidplayer.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import com.tushar.voidplayer.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class AndroidSongRepository(private val context: Context) : SongRepository {

    // ------------------------------------------------------------------
    // MediaStore path (device library scan)
    // ------------------------------------------------------------------

    override suspend fun getSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        // Filter for music files only and exclude short audio clips (< 1 sec)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 1000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                val idColumn       = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id       = cursor.getLong(idColumn)
                    val title    = cursor.getString(titleColumn)?.takeIf { it.isNotBlank() } ?: "Unknown"
                    val artist   = cursor.getString(artistColumn)?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
                    val album    = cursor.getString(albumColumn)?.takeIf { it.isNotBlank() } ?: "Unknown Album"
                    val duration = cursor.getLong(durationColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )

                    // Art is loaded lazily via ImageCache to keep initial load fast
                    songs.add(Song(id, title, artist, album, duration, contentUri.toString(), null))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("VoidPlayer", "Error querying MediaStore", e)
        }
        songs
    }

    // ------------------------------------------------------------------
    // SAF path (user-picked folder)
    // ------------------------------------------------------------------

    override suspend fun loadFromFolder(uriString: String): List<Song> = withContext(Dispatchers.IO) {
        val audioFiles = mutableListOf<androidx.documentfile.provider.DocumentFile>()
        try {
            val treeUri = android.net.Uri.parse(uriString)
            val docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)

            if (docFile != null && docFile.isDirectory) {
                collectAudioFilesIterative(docFile, audioFiles)
            }
        } catch (e: Exception) {
            android.util.Log.e("VoidPlayer", "Error loading from folder: $uriString", e)
        }

        if (audioFiles.isEmpty()) return@withContext emptyList<Song>()

        // Extract metadata concurrently across threads for fast folder loading
        coroutineScope {
            audioFiles.chunked(15).flatMap { batch ->
                batch.map { file ->
                    async(Dispatchers.IO) {
                        extractSongFromFile(file)
                    }
                }.map { it.await() }
            }
        }
    }

    private fun extractSongFromFile(
        file: androidx.documentfile.provider.DocumentFile
    ): Song {
        val fileName = file.name ?: "Unknown"
        var title  = fileName.substringBeforeLast('.').takeIf { it.isNotBlank() } ?: "Unknown"
        var artist = "Unknown Artist"
        var album  = "Unknown Album"
        var duration = 0L

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, file.uri)
            title    = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                           ?.takeIf { it.isNotBlank() } ?: title
            artist   = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                           ?.takeIf { it.isNotBlank() } ?: artist
            album    = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                           ?.takeIf { it.isNotBlank() } ?: album
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                           ?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            // Fallback to filename — already set above
        } finally {
            try { retriever.release() } catch (_: Throwable) {}
        }

        // Use a stable positive ID derived from the URI string.
        // uri.hashCode() can be negative; mask the sign bit to guarantee positive longs.
        val id = file.uri.toString().hashCode().toLong() and 0x7FFF_FFFF_FFFF_FFFFL

        return Song(
            id       = id,
            title    = title,
            artist   = artist,
            album    = album,
            duration = duration,
            uri      = file.uri.toString(),
            coverArt = null
        )
    }

    override suspend fun loadArt(uriString: String): ByteArray? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, android.net.Uri.parse(uriString))
            retriever.embeddedPicture
        } catch (e: Throwable) {
            // Catches Exception, Error (e.g. OutOfMemoryError on large embedded art),
            // and any other throwable so that art loading never crashes the app.
            android.util.Log.e("VoidPlayer", "Failed to load art for $uriString", e)
            null
        } finally {
            try { retriever.release() } catch (_: Throwable) {}
        }
    }

    // ------------------------------------------------------------------
    // Iterative (non-recursive) folder traversal to avoid stack overflow
    // on deeply nested directory structures.
    // ------------------------------------------------------------------

    private fun collectAudioFilesIterative(
        root: androidx.documentfile.provider.DocumentFile,
        audioFiles: MutableList<androidx.documentfile.provider.DocumentFile>
    ) {
        val stack = ArrayDeque<androidx.documentfile.provider.DocumentFile>()
        stack.addLast(root)

        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            val children = try { current.listFiles() } catch (_: Exception) { emptyArray() }
            for (child in children) {
                when {
                    child.isDirectory               -> stack.addLast(child)
                    isValidAudioFile(child.name)    -> audioFiles.add(child)
                }
            }
        }
    }

    private fun isValidAudioFile(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val lower = name.lowercase()
        return lower.endsWith(".mp3")  ||
               lower.endsWith(".wav")  ||
               lower.endsWith(".flac") ||
               lower.endsWith(".aac")  ||
               lower.endsWith(".ogg")  ||
               lower.endsWith(".m4a")  ||
               lower.endsWith(".opus") ||
               lower.endsWith(".wma")
    }
}
