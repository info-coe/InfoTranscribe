package com.infomericainc.infotranscribe.internal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat
import com.microsoft.cognitiveservices.speech.audio.PullAudioInputStreamCallback

internal class MicrophoneService(
    private val context: Context,
) : PullAudioInputStreamCallback() {

    private var audioStreamFormat: AudioStreamFormat? = null
    private var recorder: AudioRecord? = null

    @Throws(Exception::class)
    fun create(): MicrophoneService {
        if (audioStreamFormat != null || recorder != null) {
            Log.i(MICROPHONE_SERVICE, "No need to create active instance found")
            return this
        }

        audioStreamFormat =
            AudioStreamFormat.getWaveFormatPCM(SAMPLE_RATE, BIT_PER_SAMPLE, NUMBER_OF_CHANNELS)
        initMic()

        Log.i(MICROPHONE_SERVICE, "Created")
        return this
    }

    override fun read(dataBuffer: ByteArray?): Int {
        if (dataBuffer == null) {
            return 0
        }
        return this.recorder?.read(dataBuffer, 0, dataBuffer.size) ?: 0
    }

    private fun initMic() {
        // Note: currently, the Speech SDK support 16 kHz sample rate, 16 bit samples, mono (single-channel) only.
        val af = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE.toInt())
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Microphone permission not found")
        }

        this.recorder = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            .setAudioFormat(af)
            .build()

        recorder?.startRecording()
    }

    override fun close() {
        recorder?.release()
        recorder = null
        Log.i(MICROPHONE_SERVICE, "Microphone Service closed.")
    }

    private companion object {
        const val SAMPLE_RATE: Long = 16000.toLong()
        const val BIT_PER_SAMPLE: Short = 16.toShort()
        const val NUMBER_OF_CHANNELS: Short = 1.toShort()
        const val MICROPHONE_SERVICE: String = "MicrophoneService"
    }

}