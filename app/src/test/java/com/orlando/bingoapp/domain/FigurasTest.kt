package com.orlando.bingoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FigurasTest {

    @Test
    fun cuadritoEnCualquierPosicionSonLos16() {
        val cuadrito = Figuras.dibujo("XX...", "XX...", ".....", ".....", ".....")
        assertEquals(Figuras.CUADRITOS.toSet(), Figuras.traslaciones(cuadrito).toSet())
    }

    @Test
    fun filaEnCualquierPosicionSonLas5Filas() {
        val fila = Figuras.dibujo("XXXXX", ".....", ".....", ".....", ".....")
        assertEquals(Figuras.HORIZONTALES.toSet(), Figuras.traslaciones(fila).toSet())
    }

    @Test
    fun diagonalNoSePuedeMover() {
        assertEquals(1, Figuras.traslaciones(Figuras.DIAGONALES.first()).size)
    }

    @Test
    fun unaSolaCasillaCabeEn25Posiciones() {
        val punto = Figuras.dibujo("X....", ".....", ".....", ".....", ".....")
        assertEquals(25, Figuras.traslaciones(punto).size)
    }

    @Test
    fun expandirSinCualquierPosicionDejaLasFigurasIgual() {
        assertEquals(Figuras.CUATRO_ESQUINAS, Figuras.expandir(Figuras.CUATRO_ESQUINAS, cualquierPosicion = false))
    }

    @Test
    fun expandirJuntaFigurasSinRepetir() {
        val fila = Figuras.dibujo("XXXXX", ".....", ".....", ".....", ".....")
        val columna = Figuras.dibujo("X....", "X....", "X....", "X....", "X....")
        assertEquals(10, Figuras.expandir(listOf(fila, columna, fila), cualquierPosicion = true).size)
    }
}
