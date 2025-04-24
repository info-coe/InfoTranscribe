package com.infomericainc.infotranscribe.internal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.infomericainc.infotranscribe.api.InfoTranscribe
import com.infomericainc.infotranscribe.api.ResponseType
import com.infomericainc.infotranscribe.api.TranscribeStatus
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

internal typealias ErrorListener = (String) -> Unit
internal typealias ObserveListener = (String) -> Unit

internal class InfoTranscribeImpl(
    private val context: Context,
    apiKey: String,
    region: String,
) : InfoTranscribe {

    private var microphoneService: MicrophoneService? = null
    private var speechConfig: SpeechConfig? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private val errorListeners = mutableListOf<ErrorListener>()
    private val transcriptionListeners =
        mutableMapOf<String, Pair<ResponseType, ObserveListener>>()
    private val mutex = Mutex()
    private val jobTracker = SupervisorJob()
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        notifyError(throwable.message.toString())
    }
    private val scope = CoroutineScope(Dispatchers.Default + jobTracker + coroutineExceptionHandler)
    private var cachedResult: String = ""
    private val mutableTranscribeStatus: MutableStateFlow<TranscribeStatus> =
        MutableStateFlow(TranscribeStatus.Idle)
    override val transcribeStatus: StateFlow<TranscribeStatus> =
        mutableTranscribeStatus.asStateFlow()


    init {
        if (apiKey.isEmpty() || region.isEmpty()) {
            throw IllegalArgumentException("Api Key or regions is invalid, Unable to initialize the service.")
        }

        if (apiKey == "DUMMY_KEY") {
            Log.e(
                "LiveTranscribe",
                "Invalid api key, Make sure to change the region before you use."
            )
        } else {
            speechConfig = SpeechConfig.fromSubscription(
                apiKey, region
            )
        }
    }

    override fun startTranscribe(onStarted: () -> Unit) {
        scope.launch {
            mutex.withLock {
                if (speechConfig == null) {
                    notifyError("Unable to start the service, Make sure to initialize properly.")
                    return@launch
                }

                if (microphoneService == null) {
                    microphoneService = MicrophoneService(context = context).create()
                }

                speechRecognizer = SpeechRecognizer(
                    speechConfig,
                    AudioConfig.fromStreamInput(microphoneService)
                )

                scope.launch {
                    speechRecognizer?.startContinuousRecognitionAsync().also { it?.get() }
                    mutableTranscribeStatus.update {
                        TranscribeStatus.Transcribing
                    }
                }

                speechRecognizer?.recognized?.addEventListener { _, e ->
                    scope.launch {
                        mutex.withLock {
                            notifyResult(e.result.text)
                        }
                    }
                }
            }
        }
    }

    override fun pauseTranscribe(onPaused: () -> Unit) {
        if (speechRecognizer == null) {
            notifyError("Please call initialize, Before pausing the transcribe.")
            return
        }

        scope.launch {
            speechRecognizer?.stopContinuousRecognitionAsync().also { it?.get() }
            mutableTranscribeStatus.update {
                TranscribeStatus.Paused
            }
        }

        onPaused()
    }

    override fun addOnErrorListener(listener: ErrorListener): InfoTranscribe {
        scope.launch {
            mutex.withLock {
                errorListeners.add(listener)
            }
        }
        return this
    }

    override fun addOnObserveListener(
        responseType: ResponseType,
        observeListener: ObserveListener
    ): String {
        val listenerID = generateListenerId()
        scope.launch {
            withContext(Dispatchers.Default) {
                mutex.withLock {
                    transcriptionListeners[listenerID] = responseType to observeListener
                }
            }
        }
        return listenerID
    }

    override fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun removeActiveObservationListener(
        listenerId: String
    ) {
        scope.launch {
            mutex.withLock {
                if (transcriptionListeners.containsKey(listenerId)) {
                    transcriptionListeners.remove(listenerId)
                }
            }
        }
    }

    override fun removeActiveListeners() {
        scope.launch {
            mutex.withLock {
                errorListeners.clear()
                transcriptionListeners.clear()
            }
        }
    }

    override fun endTranscribe() {
        scope.launch {
            mutex.withLock {
                microphoneService?.close()
                speechRecognizer?.stopContinuousRecognitionAsync().also { it?.get() }
                speechRecognizer?.recognized?.removeEventListener { _, _ -> }
                speechRecognizer?.close()
                microphoneService = null
                transcriptionListeners.clear()
                errorListeners.clear()
                mutableTranscribeStatus.update {
                    TranscribeStatus.Ended
                }
            }
        }
    }

    private fun notifyError(message: String) {
        if (errorListeners.isEmpty().not()) {
            scope.launch {
                mutex.withLock {
                    errorListeners.forEach { it(message) }
                }
            }
        }
    }

    private fun notifyResult(result: String) {
        if (transcriptionListeners.isEmpty().not()) {
            transcriptionListeners.forEach { listenerId, (responseType, _) ->
                when (responseType) {
                    is ResponseType.Partial -> {
                        transcriptionListeners[listenerId]?.let { (_, listener) ->
                            listener(result)
                        }
                    }

                    is ResponseType.Continuous -> {
                        cachedResult += result.plus("\n")
                        transcriptionListeners[listenerId]?.let { (_, listener) ->
                            listener(cachedResult)
                        }
                    }
                }
            }
        }
    }

    private fun generateListenerId(): String {
        return UUID.randomUUID().toString()
    }
}