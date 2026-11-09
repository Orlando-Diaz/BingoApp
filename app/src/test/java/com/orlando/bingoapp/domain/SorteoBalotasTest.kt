package com.orlando.bingoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SorteoBalotasTest {

    @Test
    fun nuncaRepiteUnaQueYaSalio() {
        val cantadas = mutableSetOf<Int>()
        repeat(75) {
            val balota = SorteoBalotas.siguiente(cantadas, Random(42))!!
            assertTrue(balota !in cantadas)
            cantadas += balota
        }
        assertEquals((1..75).toSet(), cantadas)
    }

    @Test
    fun cuandoSalieronTodasNoHayMas() {
        assertNull(SorteoBalotas.siguiente((1..75).toSet()))
    }

    @Test
    fun siSoloFaltaUnaSaleEsa() {
        assertEquals(40, SorteoBalotas.siguiente((1..75).toSet() - 40))
    }

    @Test
    fun textoParaCantar() {
        assertEquals("B. 7", SorteoBalotas.textoParaCantar(7))
        assertEquals("G. 52", SorteoBalotas.textoParaCantar(52))
    }
}
