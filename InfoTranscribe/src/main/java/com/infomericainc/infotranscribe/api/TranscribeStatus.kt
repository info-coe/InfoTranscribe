package com.infomericainc.infotranscribe.api

/**
 * Used to update the current state of
 * the [InfoTranscribe]
 *
 * @sample com.infomericainc.infotranscribe.LiveTranscribeExample
 */
sealed class TranscribeStatus {

    /**
     * Default State of the [InfoTranscribe], This state indicates
     * that the [InfoTranscribe.initialize] is done, But not
     * yet started. Use this to update the initial State of you UI.
     */
    data object Idle : TranscribeStatus()

    /**
     * Active State of the [InfoTranscribe], This state indicates
     * that the [InfoTranscribe.startTranscribe] is called, And the
     * live transcribe is going on.
     */
    data object Transcribing : TranscribeStatus()

    /**
     * This state indicates that the [InfoTranscribe.pauseTranscribe] is called,
     * And the [InfoTranscribe] is active but pause the transcribe until [InfoTranscribe.startTranscribe]
     * is being called.
     */
    data object Paused : TranscribeStatus()

    /**
     * This state indicates that the [InfoTranscribe.endTranscribe] is called,
     * And the [InfoTranscribe] is in-active.
     */
    data object Ended : TranscribeStatus()

}