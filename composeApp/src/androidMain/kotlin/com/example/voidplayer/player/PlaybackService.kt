package com.example.voidplayer.player

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.voidplayer.VoidPlayerApp

class PlaybackService : MediaSessionService() {

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        // Guard against UninitializedPropertyAccessException. In rare cases Android may restart
        // the service process without calling Application.onCreate() first.
        return try {
            VoidPlayerApp.instance.player.getMediaSession()
        } catch (e: Throwable) {
            null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Player lifecycle is managed by the Application class for now
    }
}
