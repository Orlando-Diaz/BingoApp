package com.orlando.bingoapp.ui.seguridad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize

/**
 * Cuadrícula de 3x3 puntos para dibujar un patrón con el dedo, como el de desbloquear el celular.
 * Al levantar el dedo entrega los puntos en el orden en que se tocaron (0 a 8, fila por fila).
 */
@Composable
fun PatronDibujo(onTerminar: (List<Int>) -> Unit, modifier: Modifier = Modifier) {
    val puntos = remember { mutableStateListOf<Int>() }
    var dedo by remember { mutableStateOf<Offset?>(null) }
    val alTerminar by rememberUpdatedState(onTerminar) // siempre la versión más reciente
    val colorPunto = MaterialTheme.colorScheme.outline
    val colorActivo = MaterialTheme.colorScheme.primary

    fun centro(punto: Int, tamano: IntSize): Offset {
        val celda = tamano.width / 3f
        return Offset((punto % 3 + 0.5f) * celda, (punto / 3 + 0.5f) * celda)
    }

    /** Si el dedo pasa cerca de un punto nuevo, se agrega al patrón. */
    fun tocar(posicion: Offset, tamano: IntSize) {
        val radio = tamano.width / 3f * 0.3f
        (0..8).firstOrNull { (centro(it, tamano) - posicion).getDistance() < radio && it !in puntos }
            ?.let { puntos.add(it) }
    }

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { posicion ->
                        puntos.clear()
                        dedo = posicion
                        tocar(posicion, size)
                    },
                    onDrag = { cambio, _ ->
                        dedo = cambio.position
                        tocar(cambio.position, size)
                    },
                    onDragEnd = {
                        dedo = null
                        alTerminar(puntos.toList())
                        puntos.clear()
                    },
                    onDragCancel = {
                        dedo = null
                        puntos.clear()
                    }
                )
            }
    ) {
        val tamano = IntSize(size.width.toInt(), size.height.toInt())
        val celda = size.width / 3f

        // Líneas entre los puntos tocados, y del último punto hasta el dedo
        puntos.zipWithNext().forEach { (a, b) ->
            drawLine(colorActivo, centro(a, tamano), centro(b, tamano), strokeWidth = celda * 0.06f)
        }
        val ultimo = puntos.lastOrNull()
        val posicionDedo = dedo
        if (ultimo != null && posicionDedo != null) {
            drawLine(colorActivo, centro(ultimo, tamano), posicionDedo, strokeWidth = celda * 0.06f)
        }

        // Los 9 puntos
        for (punto in 0..8) {
            val activo = punto in puntos
            drawCircle(if (activo) colorActivo else colorPunto, radius = celda * 0.08f, center = centro(punto, tamano))
            if (activo) {
                drawCircle(colorActivo, radius = celda * 0.2f, center = centro(punto, tamano), style = Stroke(width = celda * 0.03f))
            }
        }
    }
}
