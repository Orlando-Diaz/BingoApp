package com.orlando.bingoapp.domain

import kotlin.math.abs

/** Un dígito suelto que encontró el OCR, con su caja en la foto (en píxeles). */
data class DigitoDetectado(
    val digito: Int,
    val izquierda: Float,
    val arriba: Float,
    val derecha: Float,
    val abajo: Float
) {
    val ancho: Float get() = derecha - izquierda
    val alto: Float get() = abajo - arriba
    val centroY: Float get() = (arriba + abajo) / 2
}

/**
 * Une dígitos sueltos en números.
 *
 * Problema que resuelve: en una tabla las casillas están tan juntas que ML Kit
 * lee "25 31 46 61" como un solo texto "25314661". Mirando cada dígito por separado:
 * - dígitos muy juntos -> forman un número
 * - un espacio más grande -> empieza otra casilla
 * - si aun así quedan pegados, se parten de a 2 cifras (ningún número pasa de 75)
 */
object AgrupadorDigitos {

    fun agrupar(digitos: List<DigitoDetectado>): List<NumeroDetectado> {
        if (digitos.isEmpty()) return emptyList()
        val altoTipico = mediana(digitos.map { it.alto })
        val anchoTipico = mediana(digitos.map { it.ancho })

        // 1. Separar en renglones: dígitos a la misma altura
        val renglones = mutableListOf<MutableList<DigitoDetectado>>()
        digitos.sortedBy { it.centroY }.forEach { d ->
            val renglon = renglones.lastOrNull()
                ?.takeIf { r -> abs(r.map { it.centroY }.average() - d.centroY) < altoTipico * 0.6 }
            if (renglon != null) renglon.add(d) else renglones.add(mutableListOf(d))
        }

        // 2. En cada renglón, cortar donde haya un espacio grande entre dígitos
        val numeros = mutableListOf<NumeroDetectado>()
        renglones.forEach { renglon ->
            var tramo = mutableListOf<DigitoDetectado>()
            renglon.sortedBy { it.izquierda }.forEach { d ->
                val anterior = tramo.lastOrNull()
                if (anterior != null && d.izquierda - anterior.derecha > anchoTipico * 0.45) {
                    numeros += partir(tramo)
                    tramo = mutableListOf()
                }
                tramo.add(d)
            }
            if (tramo.isNotEmpty()) numeros += partir(tramo)
        }
        return numeros
    }

    /**
     * Un tramo de dígitos pegados se parte de a 2 cifras.
     * Si tiene cantidad impar, el primero va solo: en la B hay números de una cifra
     * (ej: "925" -> 9 y 25).
     */
    private fun partir(tramo: List<DigitoDetectado>): List<NumeroDetectado> {
        val grupos = mutableListOf<List<DigitoDetectado>>()
        var i = 0
        if (tramo.size > 1 && tramo.size % 2 == 1) {
            grupos.add(listOf(tramo[0]))
            i = 1
        }
        while (i < tramo.size) {
            grupos.add(tramo.subList(i, minOf(i + 2, tramo.size)))
            i += 2
        }
        return grupos.map { g ->
            NumeroDetectado(
                valor = g.fold(0) { acumulado, d -> acumulado * 10 + d.digito },
                x = (g.first().izquierda + g.last().derecha) / 2,
                y = g.map { it.centroY }.average().toFloat(),
                alto = g.map { it.alto }.average().toFloat()
            )
        }
    }

    private fun mediana(valores: List<Float>): Float = valores.sorted()[valores.size / 2]
}
