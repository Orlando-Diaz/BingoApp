package com.orlando.bingoapp.voz

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

/** Una voz que se puede elegir en el menú. */
data class OpcionVoz(val nombre: String, val etiqueta: String)

/**
 * La voz que canta las balotas. Usa el lector de voz de Android (TextToSpeech),
 * que viene en el teléfono y funciona sin internet.
 */
class CantorBalotas(
    context: Context,
    private val vozInicial: String? = null,
    private val tonoInicial: Float = 1f
) {

    /** null mientras arranca; true si hay voz en español disponible. */
    var disponible: Boolean? = null
        private set

    private val esperando = mutableMapOf<String, () -> Unit>()

    private val voz: TextToSpeech = TextToSpeech(context.applicationContext) { estado ->
        disponible = estado == TextToSpeech.SUCCESS && configurarIdioma()
        if (disponible == true) {
            usarVoz(vozInicial)
            cambiarTono(tonoInicial)
        }
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
     * Las voces en español instaladas en el teléfono, las que no necesitan internet primero.
     * Android no dice si una voz es de hombre o de mujer: hay que escucharlas.
     */
    fun vocesEnEspanol(): List<OpcionVoz> {
        if (disponible != true) return emptyList()
        val voces = try { voz.voices } catch (e: Exception) { null } ?: return emptyList()
        val espanol = Locale.forLanguageTag("es")
        return voces
            .filter { it.locale.language == "es" && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features }
            .sortedWith(compareBy({ it.isNetworkConnectionRequired }, { it.locale.toLanguageTag() }, { it.name }))
            .mapIndexed { i, v ->
                OpcionVoz(
                    nombre = v.name,
                    etiqueta = "Voz ${i + 1} · ${v.locale.getDisplayCountry(espanol)}" +
                        if (v.isNetworkConnectionRequired) " · necesita internet" else ""
                )
            }
    }

    /** Usa la voz con ese nombre; si no existe (o es null), la de español por defecto. */
    fun usarVoz(nombre: String?) {
        if (disponible != true) return
        val elegida = nombre?.let { n -> try { voz.voices } catch (e: Exception) { null }?.find { it.name == n } }
        if (elegida != null) voz.voice = elegida else configurarIdioma()
    }

    /** 1 = normal; menos de 1 = más grave; más de 1 = más aguda. */
    fun cambiarTono(tono: Float) {
        voz.setPitch(tono)
    }

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
