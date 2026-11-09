package com.orlando.bingoapp.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatronTest {

    @Test
    fun necesitaAlMenosCuatroPuntos() {
        assertFalse(Patron.esValido(listOf(0, 1, 2)))
        assertTrue(Patron.esValido(listOf(0, 1, 2, 5)))
    }

    @Test
    fun noPuedeRepetirPuntos() {
        assertFalse(Patron.esValido(listOf(0, 1, 0, 3)))
    }

    @Test
    fun elMismoPatronCoincideYOtroNo() {
        val guardado = Patron.huella(listOf(0, 4, 8, 5))
        assertTrue(Patron.coincide(listOf(0, 4, 8, 5), guardado))
        assertFalse(Patron.coincide(listOf(5, 8, 4, 0), guardado)) // el orden importa
    }
}
