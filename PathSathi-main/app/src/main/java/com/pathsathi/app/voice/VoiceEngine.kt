package com.pathsathi.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Natural-speed Android TTS + device speech recognition wrapper. */
class VoiceEngine(context: Context) {
    private val ctx = context.applicationContext
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private var ready = false

    fun init(done: (Boolean) -> Unit = {}) {
        tts = TextToSpeech(ctx) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.setSpeechRate(1f)
                tts?.setPitch(1f)
            }
            done(ready)
        }
    }

    fun setLanguage(hindi: Boolean): Boolean {
        val result = tts?.setLanguage(if (hindi) Locale("hi", "IN") else Locale.US)
            ?: TextToSpeech.LANG_NOT_SUPPORTED
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun speak(text: String, id: String = "sathi_utt") {
        if (ready) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun stop() {
        tts?.stop()
        recognizer?.cancel()
    }

    fun shutdown() {
        recognizer?.destroy()
        recognizer = null
        tts?.shutdown()
        tts = null
    }

    fun isDeviceRecognitionAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(ctx)

    fun startListening(hindi: Boolean, onResult: (String) -> Unit, done: () -> Unit) {
        if (!isDeviceRecognitionAvailable()) {
            done()
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(ctx).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onError(error: Int) = done()
                override fun onResults(results: Bundle?) {
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?.let(onResult)
                    done()
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (hindi) "hi-IN" else "en-US")
            })
        }
    }
}
