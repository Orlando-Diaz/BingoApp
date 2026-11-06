package com.orlando.bingoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas con la tabla de la foto real que no se leía bien:
 *   B  I  N  G  O
 *   9 25 31 46 61
 *   4 18 39 53 70
 *   5 16  * 50 68
 *   2 20 34 48 66
 *  12 19 41 52 62
 */
class AgrupadorDigitosTest {

    private val tablaFoto: List<List<Int?>> = listOf(
        listOf(9, 25, 31, 46, 61),
        listOf(4, 18, 39, 53, 70),
        listOf(5, 16, null, 50, 68),
        listOf(2, 20, 34, 48, 66),
        listOf(12, 19, 41, 52, 62)
    )

    private val esperada: List<Int?> = tablaFoto.flatten()

    /**
     * Simula los dígitos que entrega ML Kit: cada dígito mide 20x30 px,
     * con 2 px entre las cifras de un mismo número, centrado en su casilla.
     * Con casillas angostas (46 px) los números quedan casi pegados entre sí.
     */
    private fun digitosDe(tabla: List<List<Int?>>, anchoCasilla: Float, arriba: Float = 100f): List<DigitoDetectado> =
        tabla.flatMapIndexed { fila, numeros ->
            numeros.flatMapIndexed { col, numero ->
                if (numero == null) emptyList() else {
                    val texto = numero.toString()
                    val anchoTexto = texto.length * 20f + (texto.length - 1) * 2f
                    val inicio = col * anchoCasilla + (anchoCasilla - anchoTexto) / 2
                    texto.mapIndexed { i, c ->
                        val izquierda = inicio + i * 22f
                        DigitoDetectado(c.digitToInt(), izquierda, arriba + fila * 50f, izquierda + 20f, arriba + fila * 50f + 30f)
                    }
                }
            }
        }

    private fun leer(digitos: List<DigitoDetectado>) = LectorTabla.armar(AgrupadorDigitos.agrupar(digitos))

    @Test
    fun casillasAnchasSeLeenCompletas() {
        assertEquals(esperada, leer(digitosDe(tablaFoto, anchoCasilla = 100f)))
    }

    @Test
    fun numerosPegadosSeSeparanBien() {
        // Así fallaba antes: "25314661" se descartaba entero
        assertEquals(esperada, leer(digitosDe(tablaFoto, anchoCasilla = 46f)))
    }

    @Test
    fun unDigitoSoloSeguidoDeDosPegados() {
        val digitos = "925".mapIndexed { i, c -> DigitoDetectado(c.digitToInt(), i * 22f, 0f, i * 22f + 20f, 30f) }
        assertEquals(listOf(9, 25), AgrupadorDigitos.agrupar(digitos).map { it.valor })
    }

    @Test
    fun elTextoCienCartasDeArribaSeIgnora() {
        // "100" impreso encima de la tabla, justo sobre la columna B
        val cien = "100".mapIndexed { i, c -> DigitoDetectado(c.digitToInt(), 5f + i * 22f, 10f, 25f + i * 22f, 40f) }
        assertEquals(esperada, leer(cien + digitosDe(tablaFoto, anchoCasilla = 46f)))
    }
}
