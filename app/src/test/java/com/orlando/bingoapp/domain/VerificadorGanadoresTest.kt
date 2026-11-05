package com.orlando.bingoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VerificadorGanadoresTest {

    /**
     * Tabla de prueba: fila f, columna c -> c*15 + f + 1. Centro libre = 0.
     *   B  I  N  G  O
     *   1 16 31 46 61
     *   2 17 32 47 62
     *   3 18  * 48 63
     *   4 19 34 49 64
     *   5 20 35 50 65
     */
    private val tabla = TablaEnJuego(
        tableroNumero = 7,
        posicion = 2,
        numeros = List(25) { i -> if (i == 12) 0 else (i % 5) * 15 + (i / 5) + 1 }
    )

    private val sencillo = listOf(
        ModoEnJuego("4 esquinas", Figuras.CUATRO_ESQUINAS),
        ModoEnJuego("Cuadrito de 4", Figuras.CUADRITOS),
        ModoEnJuego("Línea", Figuras.HORIZONTALES + Figuras.VERTICALES + Figuras.DIAGONALES)
    )

    private fun ganadores(vararg balotas: Int, modos: List<ModoEnJuego> = sencillo) =
        VerificadorGanadores.buscarGanadores(listOf(tabla), modos, balotas.toSet())

    @Test
    fun sinBalotasNoHayGanador() {
        assertTrue(ganadores().isEmpty())
    }

    @Test
    fun cuatroEsquinas() {
        val resultado = ganadores(1, 61, 5, 65)
        assertEquals(1, resultado.size)
        assertEquals("4 esquinas", resultado.first().nombreModo)
        assertEquals("Tablero 7 · tabla 2", resultado.first().descripcion)
    }

    @Test
    fun tresEsquinasNoAlcanzan() {
        assertTrue(ganadores(1, 61, 5).isEmpty())
    }

    @Test
    fun cuadritoJuntoAlCentroSoloNecesitaTresNumeros() {
        // 17, 32 y 18 + el centro libre forman un 2x2
        assertEquals("Cuadrito de 4", ganadores(17, 32, 18).first().nombreModo)
    }

    @Test
    fun lineaDiagonalUsaElCentroLibre() {
        // diagonal 1, 17, (centro), 49, 65
        assertEquals("Línea", ganadores(1, 17, 49, 65).first().nombreModo)
    }

    @Test
    fun lineaVertical() {
        assertEquals("Línea", ganadores(46, 47, 48, 49, 50).first().nombreModo)
    }

    @Test
    fun cartonLlenoNecesitaLas24() {
        val lleno = listOf(ModoEnJuego(Figuras.NOMBRE_LLENO, Figuras.LLENO))
        val todos = tabla.numeros.filter { it != 0 }
        assertTrue(ganadores(*todos.drop(1).toIntArray(), modos = lleno).isEmpty())
        assertEquals(1, ganadores(*todos.toIntArray(), modos = lleno).size)
    }

    @Test
    fun hayDieciseisCuadritos() {
        assertEquals(16, Figuras.CUADRITOS.size)
    }
}
