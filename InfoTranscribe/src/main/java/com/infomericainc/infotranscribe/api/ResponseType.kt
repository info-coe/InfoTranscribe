package com.infomericainc.infotranscribe.api

/**
 * Determines the type of response that
 * [InfoTranscribe.addOnObserveListener] has to emit. Can be
 * [ResponseType.Continuous] or [ResponseType.Partial]
 */
sealed class ResponseType {
    /**
     * Emits the partial repose of the transcribe. By adding
     * this to the [InfoTranscribe.addOnObserveListener] only the
     * latest transcribe text will be emitted. Means only the latest text.
     */
    data object Partial : ResponseType()

    /**
     * Emits the continuous repose of the transcribe. By adding
     * this to the [InfoTranscribe.addOnObserveListener] the repose
     * is continuous, Means the text from previous transcribe and current
     * transcribe.
     */
    data object Continuous : ResponseType()

}