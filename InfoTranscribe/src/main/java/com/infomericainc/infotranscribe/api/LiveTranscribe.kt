package com.infomericainc.infotranscribe.api

import android.content.Context
import androidx.annotation.Keep
import com.infomericainc.infotranscribe.internal.ErrorListener
import com.infomericainc.infotranscribe.internal.LiveTranscribeImpl
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer

@Keep
interface LiveTranscribe {

    val context: Context

    /**
     * Used to initialize the [SpeechConfig] and [SpeechRecognizer]
     * from [com.microsoft.cognitiveservices.speech]
     *
     * @param context context of the current scope.
     * @param apiKey ApiKey from the Azure Console.
     * @param region Your Azure serves region.
     *
     * Make sure to call this [initialize] first before calling any of the
     * functions from this class.
     *
     *
     */
    @Throws(IllegalArgumentException::class, SecurityException::class)
    fun initialize(
        context: Context,
        apiKey: String,
        region: String
    ): LiveTranscribe

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
     * Can be used to observe the Transcribed text from services, And make sure to
     * call the [initialize] first before calling this function. This will add the
     * [SpeechRecognizer.recognized] event listener for observing the text.
     *
     * @param onSuccess returns [String] that contains the result.
     *
     */
    fun observe(
        appendResponses : Boolean = true,
        onSuccess: (String) -> Unit,
    ) : LiveTranscribe

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
     * [startTranscribe],[observe],[endTranscribe],[pauseTranscribe],
     * Make sure to attach this to observe any kind of
     * issues.
     */
    fun addOnErrorListener(listener: ErrorListener): LiveTranscribe


    /**
     * This will end the transcribe services,
     * And end the MicrophoneServices. Call this to end the
     * services completely.
     */
    fun endTranscribe()

    /**
     * Check weather has microphone permission or not.
     */
    fun hasMicrophonePermission(): Boolean


    companion object {
        /**
         * Create the instance of [LiveTranscribe], Make sure
         * to call [initialize] first before using any of the
         * calls.
         */
        @JvmStatic
        fun getTranscribe(context: Context): LiveTranscribe =
            LiveTranscribeImpl(context = context)
    }

}