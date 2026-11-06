package com.orlando.bingoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** La casilla LIBRE del centro nunca sirve para ganar. */
class FigurasTest {

    private val cuadrito = Figuras.dibujo("XX...", "XX...", ".....", ".....", ".....")
    private val fila = Figuras.dibujo("XXXXX", ".....", ".....", ".....", ".....")
    private val columna = Figuras.dibujo("X....", "X....", "X....", "X....", "X....")

    @Test
    fun cuadritoEnCualquierPosicionSonSolo12() {
        val posiciones = Figuras.expandir(listOf(cuadrito), cualquierPosicion = true)
        assertEquals(12, posiciones.size)
        assertTrue(posiciones.none { Figuras.usaCentro(it) })
        // Son los 16 cuadritos menos los 4 que tocan el centro
        assertEquals(Figuras.CUADRITOS.filterNot { Figuras.usaCentro(it) }.toSet(), posiciones.toSet())
    }

    @Test
    fun filaEnCualquierPosicionSonLas4FilasSinLaDelMedio() {
        val posiciones = Figuras.expandir(listOf(fila), cualquierPosicion = true)
        assertEquals(4, posiciones.size)
        assertTrue(posiciones.none { Figuras.usaCentro(it) })
    }

    @Test
    fun traslacionesSinFiltroSiguenSiendoTodas() {
        assertEquals(16, Figuras.traslaciones(cuadrito).size)
        assertEquals(5, Figuras.traslaciones(fila).size)
    }

    @Test
    fun unaSolaCasillaCabeEn24Posiciones() {
        val punto = Figuras.dibujo("X....", ".....", ".....", ".....", ".....")
        assertEquals(24, Figuras.expandir(listOf(punto), cualquierPosicion = true).size)
    }

    @Test
    fun expandirSinCualquierPosicionDejaLasFigurasIgual() {
        assertEquals(Figuras.CUATRO_ESQUINAS, Figuras.expandir(Figuras.CUATRO_ESQUINAS, cualquierPosicion = false))
    }

    @Test
    fun expandirJuntaFigurasSinRepetir() {
        // 4 filas + 4 columnas (las del medio pasan por la LIBRE)
        assertEquals(8, Figuras.expandir(listOf(fila, columna, fila), cualquierPosicion = true).size)
    }

    @Test
    fun laLibreNoSeCuentaComoCasilla() {
        assertEquals(24, Figuras.cantidadCasillas(Figuras.LLENO.first()))
    }
}
