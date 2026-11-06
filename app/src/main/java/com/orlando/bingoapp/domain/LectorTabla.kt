package com.orlando.bingoapp.domain

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Un número que el OCR encontró en la foto, con el centro de su posición (en píxeles).
 * alto: altura del texto; sirve para saber qué tan lejos es "lejos" en esa foto.
 */
data class NumeroDetectado(val valor: Int, val x: Float, val y: Float, val alto: Float = 0f)

/**
 * Arma las 25 casillas de una tabla a partir de los números leídos en la foto.
 *
 * La idea clave: la COLUMNA la dice el valor (un 7 solo puede ir en la B, un 52 en la G),
 * así que no dependemos de que la foto esté derecha. La FILA se saca por la altura.
 *
 * Devuelve null en las casillas que no se pudieron leer (y en el centro libre).
 */
object LectorTabla {

    fun armar(detectados: List<NumeroDetectado>): List<Int?> {
        val validos = detectados.filter { it.valor in 1..75 }
        val altoTipico = validos.map { it.alto }.filter { it > 0f }.let { if (it.isEmpty()) 0f else mediana(it) }

        // 1. Agrupar por columna según el valor y quedarse con los que son de la tabla
        val columnas: Map<Int, List<NumeroDetectado>> = (0 until 5).associateWith { col ->
            elegir(
                candidatos = validos.filter { (it.valor - 1) / 15 == col },
                esperados = filasDe(col).size,
                altoTipico = altoTipico
            )
        }

        // 2. Altura de cada fila
        val alturas = alturasPorColumnasCompletas(columnas)
            .takeIf { a -> a.any { it != null } }
            ?: alturasPorAgrupamiento(columnas.values.flatten(), altoTipico)

        // 3. Ubicar cada número en su fila
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

    /**
     * De los números de una columna, se queda con los que de verdad son de la tabla.
     * Sobran, por ejemplo, el "1" de "100 CARTAS" o números de otra tabla que salió en la foto.
     */
    private fun elegir(
        candidatos: List<NumeroDetectado>,
        esperados: Int,
        altoTipico: Float
    ): List<NumeroDetectado> {
        var lista = candidatos.sortedBy { it.y }
        if (lista.size > esperados && altoTipico > 0f) {
            // Fuera: los que están corridos de lado respecto al resto de la columna
            val medianaX = mediana(lista.map { it.x })
            lista = lista.filter { abs(it.x - medianaX) <= altoTipico * 2.5f }
        }
        if (lista.size > esperados) {
            // Los números de la tabla están juntos: se toma el bloque seguido más compacto
            val inicio = (0..lista.size - esperados).minBy { i -> lista[i + esperados - 1].y - lista[i].y }
            lista = lista.subList(inicio, inicio + esperados)
        }
        return lista.distinctBy { it.valor }
    }

    /** Altura de cada fila usando las columnas que se leyeron completas. */
    private fun alturasPorColumnasCompletas(columnas: Map<Int, List<NumeroDetectado>>): List<Double?> {
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
        return (0 until 5).map { if (cuentas[it] > 0) sumas[it] / cuentas[it] else null }
    }

    /**
     * Si ninguna columna salió completa: se agrupan TODOS los números por altura.
     * Cada grupo es una fila; la distancia entre filas dice qué filas faltan.
     */
    private fun alturasPorAgrupamiento(numeros: List<NumeroDetectado>, altoTipico: Float): List<Double?> {
        val vacio = List<Double?>(5) { null }
        if (numeros.isEmpty() || altoTipico <= 0f) return vacio

        val grupos = mutableListOf<MutableList<Float>>()
        numeros.map { it.y }.sorted().forEach { y ->
            val ultimo = grupos.lastOrNull()
            if (ultimo != null && y - ultimo.average() < altoTipico * 0.7f) ultimo.add(y)
            else grupos.add(mutableListOf(y))
        }
        val centros = grupos.map { it.average() }
        if (centros.size < 2) return vacio

        // El salto más pequeño entre grupos es lo que mide una fila
        val paso = centros.zipWithNext { a, b -> b - a }.minOrNull() ?: return vacio
        val indices = centros.map { ((it - centros.first()) / paso).roundToInt() }
        if (indices.last() > 4) return vacio

        val alturas = MutableList<Double?>(5) { null }
        indices.forEachIndexed { i, fila -> alturas[fila] = centros[i] }
        // Completar las filas que faltan con el paso
        for (fila in 0 until 5) {
            if (alturas[fila] == null) alturas[fila] = centros.first() + paso * fila
        }
        return alturas
    }

    /** Filas que tienen número en cada columna (la N no tiene la del centro). */
    private fun filasDe(columna: Int): List<Int> =
        if (columna == 2) listOf(0, 1, 3, 4) else listOf(0, 1, 2, 3, 4)

    private fun mediana(valores: List<Float>): Float = valores.sorted()[valores.size / 2]
}
