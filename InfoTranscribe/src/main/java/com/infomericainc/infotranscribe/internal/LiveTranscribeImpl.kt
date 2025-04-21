package com.infomericainc.infotranscribe.internal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.infomericainc.infotranscribe.api.LiveTranscribe
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal typealias ErrorListener = (String) -> Unit

internal class LiveTranscribeImpl(
    override val context: Context
) : LiveTranscribe {

    private var microphoneService: MicrophoneService? = null
    private var speechConfig: SpeechConfig? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private val errorListeners = mutableListOf<ErrorListener>()
    private val pendingErrors = mutableListOf<String>()
    private val jobTracker = SupervisorJob()
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        notifyError(throwable.message.toString())
    }
    private val scope = CoroutineScope(Dispatchers.Default + jobTracker + coroutineExceptionHandler)

    override fun addOnErrorListener(listener: ErrorListener): LiveTranscribe {
        errorListeners.add(listener)
        pendingErrors.forEach { listener(it) }
        Log.i(LIVE_TRANSCRIBE, "ErrorListener registered.")
        return this
    }

    override fun initialize(
        context: Context,
        apiKey: String,
        region: String,
    ): LiveTranscribe {

        if (apiKey.isEmpty() || region.isEmpty()) {
            throw IllegalArgumentException("Api Key or regions is invalid, Unable to initialize the service.")
        }

        speechConfig = SpeechConfig.fromSubscription(
            apiKey, region
        )

        //Throws exception
        microphoneService = MicrophoneService(
            context = context
        ).create()

        speechRecognizer = SpeechRecognizer(
            speechConfig,
            AudioConfig.fromStreamInput(microphoneService)
        )

        Log.i(LIVE_TRANSCRIBE, "Initialized")
        return this
    }

    override fun startTranscribe(onStarted: () -> Unit) {
        if (speechRecognizer == null) {
            notifyError("Please call initialize, Before starting the transcribe.")
            return
        }

        scope.launch {
            speechRecognizer?.startContinuousRecognitionAsync().also { it?.get() }
            onStarted()
        }

        Log.i(LIVE_TRANSCRIBE, "Starting the live transcription.")
    }

    override fun observe(
        appendResponses: Boolean,
        onSuccess: (String) -> Unit
    ): LiveTranscribe {
        if (speechRecognizer == null) {
            notifyError("Please call initialize, Before observing the transcribe.")
            return this
        }

        var response = ""
        speechRecognizer?.recognized?.addEventListener { _, e ->
            if (appendResponses) {
                response += e.result.text
                onSuccess(response)
            } else {
                onSuccess(e.result.text)
            }
        }
        return this
    }

    private fun notifyError(message: String) {
        if (errorListeners.isEmpty()) {
            pendingErrors.add(message)
        } else {
            errorListeners.forEach { it(message) }
        }
    }


    override fun pauseTranscribe(onPaused: () -> Unit) {
        if (speechRecognizer == null) {
            notifyError("Please call initialize, Before pausing the transcribe.")
            return
        }

        scope.launch {
            speechRecognizer?.stopContinuousRecognitionAsync().also { it?.get() }
        }

        onPaused()
        Log.i(LIVE_TRANSCRIBE, "Pausing the live transcription.")
    }

    override fun endTranscribe() {
        scope.launch {
            microphoneService?.close()
            speechRecognizer?.recognized?.removeEventListener { sender, e -> }
            speechRecognizer?.close()
            speechConfig?.close()
            speechConfig = null
            microphoneService = null
            pendingErrors.clear()
        }
        Log.i(LIVE_TRANSCRIBE, "Live transcription Ended.")
    }

    override fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }


    private companion object {
        private const val LIVE_TRANSCRIBE = "LiveTranscribe"
    }
}