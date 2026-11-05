package com.orlando.bingoapp.domain

import kotlin.math.abs

/** Un número que el OCR encontró en la foto, con el centro de su posición (en píxeles). */
data class NumeroDetectado(val valor: Int, val x: Float, val y: Float)

/**
 * Arma las 25 casillas de una tabla a partir de los números leídos en la foto.
 *
 * La idea clave: la COLUMNA la dice el valor (un 7 solo puede ir en la B, un 52 en la G),
 * así que no dependemos de que la foto esté derecha. La FILA se saca ordenando por altura.
 *
 * Devuelve null en las casillas que no se pudieron leer (y en el centro libre).
 */
object LectorTabla {

    fun armar(detectados: List<NumeroDetectado>): List<Int?> {
        // 1. Agrupar por columna según el valor (sin repetidos)
        val porColumna = detectados
            .filter { it.valor in 1..75 }
            .distinctBy { it.valor }
            .groupBy { (it.valor - 1) / 15 }

        // 2. Si una columna tiene más números de los que caben, sobran los que están
        //    lejos horizontalmente del resto (ej: un número de serie impreso en la tabla)
        val columnas: Map<Int, List<NumeroDetectado>> = (0 until 5).associateWith { col ->
            val candidatos = porColumna[col].orEmpty()
            val esperados = filasDe(col).size
            val elegidos = if (candidatos.size <= esperados) candidatos else {
                val medianaX = candidatos.map { it.x }.sorted()[candidatos.size / 2]
                candidatos.sortedBy { abs(it.x - medianaX) }.take(esperados)
            }
            elegidos.sortedBy { it.y }
        }

        // 3. Altura de cada fila, usando las columnas que se leyeron completas
        val sumas = DoubleArray(5)
        val cuentas = IntArray(5)
        columnas.forEach { (col, numeros) ->
            val filas = filasDe(col)
            if (numeros.size == filas.size) {
                numeros.forEachIndexed { i, n ->
                    sumas[filas[i]] += n.y.toDouble()
                    cuentas[filas[i]]++
                }
            }
        }
        val alturas: List<Double?> = (0 until 5).map { if (cuentas[it] > 0) sumas[it] / cuentas[it] else null }

        // 4. Ubicar cada número en su fila
        val resultado = MutableList<Int?>(25) { null }
        columnas.forEach { (col, numeros) ->
            val filas = filasDe(col)
            if (numeros.size == filas.size || alturas.all { it == null }) {
                // Columna completa (o sin referencias): en orden de arriba hacia abajo
                numeros.forEachIndexed { i, n -> if (i < filas.size) resultado[filas[i] * 5 + col] = n.valor }
            } else {
                // Columna incompleta: cada número va a la fila de altura más parecida
                val libres = filas.toMutableList()
                numeros.forEach { n ->
                    val fila = libres
                        .filter { alturas[it] != null }
                        .minByOrNull { abs(alturas[it]!! - n.y) }
                        ?: libres.firstOrNull()
                    if (fila != null) {
                        resultado[fila * 5 + col] = n.valor
                        libres.remove(fila)
                    }
                }
            }
        }
        return resultado
    }

    /** Filas que tienen número en cada columna (la N no tiene la del centro). */
    private fun filasDe(columna: Int): List<Int> =
        if (columna == 2) listOf(0, 1, 3, 4) else listOf(0, 1, 2, 3, 4)
}
