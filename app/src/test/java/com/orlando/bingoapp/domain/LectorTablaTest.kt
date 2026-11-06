package com.orlando.bingoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LectorTablaTest {

    /** La misma tabla de los otros tests: fila f, columna c -> c*15 + f + 1. */
    private val esperada: List<Int?> = List(25) { i -> if (i == 12) null else (i % 5) * 15 + (i / 5) + 1 }

    /** Simula lo que leería el OCR de una foto derecha: cada casilla mide 100x100 px. */
    private fun fotoPerfecta(): List<NumeroDetectado> = esperada.mapIndexedNotNull { i, valor ->
        valor?.let { NumeroDetectado(it, x = (i % 5) * 100f + 50f, y = (i / 5) * 100f + 200f) }
    }

    @Test
    fun fotoPerfectaSeLeeCompleta() {
        assertEquals(esperada, LectorTabla.armar(fotoPerfecta()))
    }

    @Test
    fun elOrdenDeLecturaNoImporta() {
        assertEquals(esperada, LectorTabla.armar(fotoPerfecta().shuffled()))
    }

    @Test
    fun siFaltaUnNumeroQuedaVacioEnSuLugarCorrecto() {
        // No se leyó el 3 (columna B, fila del medio)
        val resultado = LectorTabla.armar(fotoPerfecta().filter { it.valor != 3 })
        assertNull(resultado[10])
        assertEquals(4, resultado[15]) // el 4 sigue en su fila, no "sube"
        assertEquals(5, resultado[20])
    }

    @Test
    fun numeroDeSerieLejosDeLaColumnaSeIgnora() {
        // Un "9" impreso en una esquina de la tabla, lejos de la columna B
        val conRuido = fotoPerfecta() + NumeroDetectado(9, x = 480f, y = 50f)
        assertEquals(esperada, LectorTabla.armar(conRuido))
    }

    @Test
    fun fotoUnPocoTorcidaIgualFunciona() {
        // Cada columna un poco más abajo que la anterior (foto inclinada)
        val torcida = fotoPerfecta().map { it.copy(y = it.y + it.x * 0.15f) }
        assertEquals(esperada, LectorTabla.armar(torcida))
    }

    @Test
    fun numerosFueraDeRangoSeIgnoran() {
        val conBasura = fotoPerfecta() + NumeroDetectado(0, 10f, 10f) + NumeroDetectado(99, 20f, 20f)
        assertEquals(esperada, LectorTabla.armar(conBasura))
    }

    @Test
    fun conPocosNumerosLeidosIgualQuedanEnSuFila() {
        // Lo que pasó con la foto real: solo se leyeron 7 números y ninguna columna completa.
        // Antes el 5 (fila del medio) quedaba arriba; ahora las filas se sacan por altura.
        fun n(valor: Int, fila: Int, col: Int) = NumeroDetectado(valor, col * 100f + 50f, fila * 100f + 200f, alto = 40f)
        val leidos = listOf(n(5, 2, 0), n(2, 3, 0), n(16, 2, 1), n(50, 2, 3), n(61, 0, 4), n(68, 2, 4), n(62, 4, 4))

        val resultado = LectorTabla.armar(leidos)

        val esperado = MutableList<Int?>(25) { null }
        leidos.forEach { esperado[(((it.y - 200f) / 100f).toInt()) * 5 + ((it.x - 50f) / 100f).toInt()] = it.valor }
        assertEquals(esperado, resultado)
    }
}
