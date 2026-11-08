package com.orlando.bingoapp.camara

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orlando.bingoapp.domain.ReglasBingo
import java.io.File

/**
 * Botón para tomar la foto de una tabla con la app de cámara del teléfono.
 * Se usa en el registro de tableros y en el editor de una tabla.
 *
 * @param onPreparar crea el archivo y devuelve la dirección donde la cámara guardará la foto.
 * @param onResultado true si la persona tomó y aceptó la foto.
 */
@Composable
fun BotonFotoTabla(
    leyendo: Boolean,
    tieneFoto: Boolean,
    mensaje: String?,
    onPreparar: () -> Uri,
    onResultado: (Boolean) -> Unit
) {
    // Contrato TakePicture: abre la cámara y guarda la foto en la dirección que le damos
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), onResultado)

    if (leyendo) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator()
            Text("Leyendo los números…")
        }
    } else {
        OutlinedButton(
            onClick = {
                try {
                    camara.launch(onPreparar())
                } catch (e: ActivityNotFoundException) {
                    onResultado(false)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (tieneFoto) "📷  Tomar otra foto" else "📷  Tomar foto de esta tabla", fontSize = 18.sp)
        }
        Text("Encuadra solo esta tabla, derecha y con buena luz.", style = MaterialTheme.typography.bodySmall)
    }
    mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
}

/**
 * Lee la foto y escribe los números en las casillas (las no leídas quedan vacías).
 * Devuelve el mensaje para mostrarle a la persona.
 */
suspend fun LectorFotoTabla.llenarCasillas(archivo: File, casillas: MutableList<String>): String {
    val numeros = leer(archivo)
    numeros.forEachIndexed { i, n ->
        if (i != ReglasBingo.INDICE_CENTRO) casillas[i] = n?.toString().orEmpty()
    }
    val leidos = numeros.count { it != null }
    return if (leidos == 24) "Se leyeron los 24 números. Compáralos con la foto antes de guardar."
    else "Se leyeron $leidos de 24. Completa las casillas en rojo mirando la foto."
}

const val MENSAJE_FOTO_FALLIDA = "No se pudo leer la foto. Intenta de nuevo con más luz o escribe los números."
