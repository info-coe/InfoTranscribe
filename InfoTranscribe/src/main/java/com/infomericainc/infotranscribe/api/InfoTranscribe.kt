package com.infomericainc.infotranscribe.api

import android.content.Context
import androidx.annotation.Keep
import com.infomericainc.infotranscribe.internal.ErrorListener
import com.infomericainc.infotranscribe.internal.InfoTranscribeImpl
import com.infomericainc.infotranscribe.internal.ObserveListener
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import kotlinx.coroutines.flow.StateFlow

@Keep
interface InfoTranscribe {

    /**
     * Used to get the current [TranscribeStatus],
     * Calling [InfoTranscribe.initialize] will emit the - [TranscribeStatus.Idle],
     * Calling [InfoTranscribe.startTranscribe] will emit the - [TranscribeStatus.Transcribing],
     * Calling [InfoTranscribe.pauseTranscribe] will emit the - [TranscribeStatus.Paused],
     * Calling [InfoTranscribe.endTranscribe] will emit the - [TranscribeStatus.Ended]
     *
     * Make sure to call these according to update your ui state.
     */
    val transcribeStatus: StateFlow<TranscribeStatus>

    /**
     * Can be used to start the Speech Reorganization from
     * Services, Make sure to call the [initialize] first
     * before calling this.
     *
     * @param onStarted can be used to updated the state of
     * the application.
     *
     * Register to [addOnErrorListener] for any error's during
     * the initialization of Transcribe.
     *
     */
    fun startTranscribe(
        onStarted: () -> Unit = { }
    )


    /**
     * Can be used to pause the transcribe Services, And Make sure to call
     * the [initialize] first before calling this function, This will pause
     * the current active transcription.
     */
    fun pauseTranscribe(
        onPaused: () -> Unit = { }
    )

    /**
     * Used to observe errors, from
     * [startTranscribe],[endTranscribe],[pauseTranscribe],
     * Make sure to attach this to observe any kind of
     * issues.
     */
    fun addOnErrorListener(listener: ErrorListener): InfoTranscribe


    /**
     * This will end the transcribe services,
     * And end the MicrophoneServices. Call this to end the
     * services completely. Make sure to call this
     * while ending the screen or at the end of your lifecycle.
     */
    fun endTranscribe()

    /**
     * Check weather has microphone permission or not.
     */
    fun hasMicrophonePermission(): Boolean

    /**
     * Can be used to observe the Transcribed text from services, And make sure to
     * call the [initialize] first before calling this function. This will add the
     * [SpeechRecognizer.recognized] event listener for observing the text.
     *
     * @param responseType type of response that observer has to emit. By default
     * the observer will emit [ResponseType.Continuous]
     * @param observeListener callback to gather the transcribed text
     *
     * @return active id [String] of the listener.
     *
     * @sample com.infomericainc.infotranscribe.LiveTranscribeExample
     */
    fun addOnObserveListener(
        responseType: ResponseType = ResponseType.Continuous,
        observeListener: ObserveListener
    ): String

    /**
     * Can be used to remove remove the active [InfoTranscribe.addOnObserveListener] by using
     * the id.
     *
     * @param listenerId active id of [InfoTranscribe.addOnObserveListener]
     */
    fun removeActiveObservationListener(
        listenerId: String
    )

    /**
     * Used to remove all the [InfoTranscribe.addOnObserveListener] listeners,
     * This will be triggered by [InfoTranscribe.endTranscribe]
     */
    fun removeActiveListeners()


    companion object {

        @Volatile
        private var instance: InfoTranscribe? = null

        /**
         * Used to initialize the [SpeechConfig] and [SpeechRecognizer]
         * from [com.microsoft.cognitiveservices.speech]
         *
         * @param context context of the current scope.
         * @param apiKey ApiKey from the Azure Console.
         * @param region Your Azure serves region.
         *
         * Make sure to call this [initialize] first in Application class.
         *
         * @sample com.infomericainc.infotranscribe.InfoTranscribeApplication
         *
         *
         */
        @Throws(IllegalArgumentException::class, SecurityException::class)
        fun initialize(
            context: Context,
            apiKey: String,
            region: String
        ) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        try {
                            instance = InfoTranscribeImpl(
                                context, apiKey, region
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        /**
         * Create the instance of [InfoTranscribe], Make sure
         * to call [initialize] first in your application class before calling
         * this.
         */
        @JvmStatic
        fun getTranscribe(): InfoTranscribe {
            return instance ?: throw IllegalStateException(
                "LiveTranscribe is not initialized. Call initialize() first in your Application class."
            )
        }

    }

}