package com.pathsathi.app.voice

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Thin wrapper over Android's built-in TextToSpeech (offline-capable when the
 * device has the language's TTS data installed) and SpeechRecognizer.
 * Speech rate is kept at natural/normal speed (1.0f) per the "no robotic voice"
 * requirement. No external voice service is used, so basic voice works offline
 * whenever the device already has the selected language's voice data.
 */
class VoiceEngine(context: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val appContext = context.applicationContext

    fun init(onReady: (Boolean) -> Unit = {}) {
        tts = TextToSpeech(appContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.setSpeechRate(1.0f) // natural speed, never sped up/slowed down
                tts?.setPitch(1.0f)
            }
            onReady(ttsReady)
        }
    }

    /** Returns true if the language switched successfully; false if the device is missing that voice data. */
    fun setLanguage(isHindi: Boolean): Boolean {
        val locale = if (isHindi) Locale("hi", "IN") else Locale.US
        val result = tts?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun speak(text: String, utteranceId: String = "sathi_utt") {
        if (!ttsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun setProgressListener(onDone: (String) -> Unit) {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { utteranceId?.let(onDone) }
            override fun onError(utteranceId: String?) {}
        })
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    fun isDeviceRecognitionAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun buildRecognizerIntent(isHindi: Boolean): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isHindi) "hi-IN" else "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
    }
}
