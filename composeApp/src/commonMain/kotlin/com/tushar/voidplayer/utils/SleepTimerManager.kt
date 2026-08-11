package com.tushar.voidplayer.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SleepTimerManager {
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    var stopAtEndOfSong: Boolean = false
        private set

    fun startTimer(minutes: Int, onTimerFinished: () -> Unit) {
        cancel()
        stopAtEndOfSong = false
        if (minutes <= 0) return

        val totalSeconds = minutes * 60L
        _remainingSeconds.value = totalSeconds
        _isActive.value = true

        timerJob = scope.launch {
            var left = totalSeconds
            while (left > 0) {
                delay(1000)
                left--
                _remainingSeconds.value = left
            }
            _isActive.value = false
            onTimerFinished()
        }
    }

    fun startEndOfSongTimer() {
        cancel()
        stopAtEndOfSong = true
        _isActive.value = true
        _remainingSeconds.value = 0L
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        stopAtEndOfSong = false
        _isActive.value = false
        _remainingSeconds.value = 0L
    }
}
