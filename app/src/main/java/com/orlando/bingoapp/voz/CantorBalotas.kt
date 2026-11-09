package com.orlando.bingoapp.voz

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

/**
 * La voz que canta las balotas. Usa el lector de voz de Android (TextToSpeech),
 * que viene en el teléfono y funciona sin internet.
 */
class CantorBalotas(context: Context) {

    /** null mientras arranca; true si hay voz en español disponible. */
    var disponible: Boolean? = null
        private set

    private val esperando = mutableMapOf<String, () -> Unit>()

    private val voz: TextToSpeech = TextToSpeech(context.applicationContext) { estado ->
        disponible = estado == TextToSpeech.SUCCESS && configurarIdioma()
    }

    init {
        voz.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String) {}
            override fun onDone(id: String) = terminar(id)
            @Deprecated("Requerido por la clase base")
            override fun onError(id: String) = terminar(id)
            override fun onError(id: String, codigo: Int) = terminar(id)
        })
    }

    /** Español de Colombia; si el teléfono no lo tiene, cualquier español. */
    private fun configurarIdioma(): Boolean =
        listOf(Locale.forLanguageTag("es-CO"), Locale.forLanguageTag("es-US"), Locale.forLanguageTag("es"))
            .any { idioma -> voz.setLanguage(idioma) >= TextToSpeech.LANG_AVAILABLE }
            .also { if (it) voz.setSpeechRate(0.85f) } // un poco más despacio, para que se entienda bien

    /**
     * Dice el texto y espera a que termine de hablar (máximo 8 segundos).
     * Así la siguiente balota no sale mientras todavía se está cantando la anterior.
     */
    suspend fun decir(texto: String) {
        if (disponible != true) return
        val id = UUID.randomUUID().toString()
        withTimeoutOrNull(8_000) {
            suspendCancellableCoroutine { continuacion ->
                synchronized(esperando) { esperando[id] = { continuacion.resume(Unit) } }
                continuacion.invokeOnCancellation { synchronized(esperando) { esperando.remove(id) } }
                voz.speak(texto, TextToSpeech.QUEUE_FLUSH, null, id)
            }
        }
    }

    private fun terminar(id: String) {
        val accion = synchronized(esperando) { esperando.remove(id) }
        accion?.invoke()
    }

    fun callar() = voz.stop()

    fun cerrar() {
        voz.stop()
        voz.shutdown()
    }
}
