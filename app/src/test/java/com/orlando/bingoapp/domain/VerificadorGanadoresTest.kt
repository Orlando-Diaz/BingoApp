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

    /** Las jugadas como las crean en la app: dibujadas una vez y "en cualquier posición". */
    private val sencillo = listOf(
        ModoEnJuego("4 esquinas", Figuras.CUATRO_ESQUINAS),
        ModoEnJuego(
            "Cuadrito de 4",
            Figuras.expandir(listOf(Figuras.dibujo("XX...", "XX...", ".....", ".....", ".....")), cualquierPosicion = true)
        ),
        ModoEnJuego(
            "Línea",
            Figuras.expandir(
                listOf(
                    Figuras.dibujo("XXXXX", ".....", ".....", ".....", "....."),
                    Figuras.dibujo("X....", "X....", "X....", "X....", "X....")
                ),
                cualquierPosicion = true
            )
        )
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
    fun cuadritoQueTocaLaLibreNoGana() {
        // 17, 32 y 18 + la LIBRE: antes ganaba, pero la LIBRE nunca cuenta
        assertTrue(ganadores(17, 32, 18).isEmpty())
    }

    @Test
    fun cuadritoNormalGana() {
        // 1, 16, 2, 17: esquina de arriba a la izquierda
        assertEquals("Cuadrito de 4", ganadores(1, 16, 2, 17).first().nombreModo)
    }

    @Test
    fun lineaDelMedioNoGanaPorqueTieneLaLibre() {
        // fila del medio: 3, 18, (LIBRE), 48, 63
        assertTrue(ganadores(3, 18, 48, 63).isEmpty())
    }

    @Test
    fun figuraDibujadaQuePasaPorElCentroLoIgnora() {
        // Una X de la llena: se gana con sus 8 números, la LIBRE no se exige
        val x = listOf(ModoEnJuego("La X", Figuras.X))
        assertEquals("La X", ganadores(1, 17, 49, 65, 61, 47, 19, 5, modos = x).first().nombreModo)
        assertTrue(ganadores(1, 17, 49, 65, 61, 47, 19, modos = x).isEmpty())
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

    @Test
    fun diceConQueBalotaCompleto() {
        // Las 4 esquinas se completan con la 4ª balota; después salen más
        val orden = listOf(1, 61, 30, 5, 65, 40, 70)
        val resultado = VerificadorGanadores.buscarConMomento(listOf(tabla), sencillo, orden)
        assertEquals("4 esquinas", resultado.first().nombreModo)
        assertEquals(5, resultado.first().balotasAlCompletar) // 1, 61, 30, 5, 65 -> la 65 sale de 5ª
    }

    @Test
    fun siCompletaDosFigurasCuentaLaPrimera() {
        // Columna G completa con la 5ª balota; 4 esquinas con la 8ª
        val orden = listOf(46, 47, 48, 49, 50, 1, 61, 5, 65)
        val resultado = VerificadorGanadores.buscarConMomento(listOf(tabla), sencillo, orden)
        assertEquals("Línea", resultado.first().nombreModo)
        assertEquals(5, resultado.first().balotasAlCompletar)
    }

    @Test
    fun sinCompletarNoAparece() {
        assertTrue(VerificadorGanadores.buscarConMomento(listOf(tabla), sencillo, listOf(1, 61, 5)).isEmpty())
    }
}
