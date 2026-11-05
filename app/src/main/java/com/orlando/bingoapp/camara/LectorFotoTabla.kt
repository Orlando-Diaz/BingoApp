package com.orlando.bingoapp.camara

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.orlando.bingoapp.domain.LectorTabla
import com.orlando.bingoapp.domain.NumeroDetectado
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Lee los números de la foto de una tabla con ML Kit (en el teléfono, sin internet)
 * y se los pasa a LectorTabla para armar la cuadrícula.
 */
class LectorFotoTabla(private val context: Context) {

    private val reconocedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Crea un archivo vacío donde la cámara guardará la foto. */
    fun nuevoArchivo(): File {
        val carpeta = File(context.filesDir, "fotos").apply { mkdirs() }
        return File(carpeta, "tabla_${System.currentTimeMillis()}.jpg")
    }

    /** Dirección "content://" que se le pasa a la app de cámara. */
    fun uriPara(archivo: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)

    suspend fun leer(archivo: File): List<Int?> {
        // fromFilePath respeta la orientación de la foto (si se tomó de lado)
        val imagen = InputImage.fromFilePath(context, uriPara(archivo))
        val texto = reconocedor.process(imagen).esperar()

        val detectados = texto.textBlocks
            .flatMap { it.lines }
            .flatMap { it.elements }
            .flatMap { elemento ->
                val caja = elemento.boundingBox ?: return@flatMap emptyList()
                // A veces ML Kit junta dos números ("12 27"): se separan por lo que no sea dígito
                elemento.text.split(Regex("\\D+"))
                    .filter { it.length in 1..2 }
                    .mapNotNull { it.toIntOrNull() }
                    .map { NumeroDetectado(it, caja.exactCenterX(), caja.exactCenterY()) }
            }
        return LectorTabla.armar(detectados)
    }

    fun cerrar() = reconocedor.close()
}

/**
 * ML Kit devuelve un Task (estilo callbacks). Esta función lo convierte en una
 * función suspend para usarlo con corrutinas, como si fuera código normal.
 */
private suspend fun <T> Task<T>.esperar(): T = suspendCancellableCoroutine { continuacion ->
    addOnSuccessListener { continuacion.resume(it) }
    addOnFailureListener { continuacion.resumeWithException(it) }
}
