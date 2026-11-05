package com.orlando.bingoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DineroTest {

    @Test
    fun preciosDelSencillo() {
        assertEquals(500, Dinero.valorTablas(4, 500))
        assertEquals(1000, Dinero.valorTablas(8, 500))
    }

    @Test
    fun preciosDeLaLlena() {
        assertEquals(1000, Dinero.valorTablas(4, 1000))
        assertEquals(2000, Dinero.valorTablas(8, 1000))
    }

    @Test
    fun seisTablasDeUnTableroDeOcho() {
        assertEquals(750, Dinero.valorTablas(6, 500))
    }

    @Test
    fun porcentajeDelPremio() {
        assertEquals(20_000, Dinero.porcentaje(100_000, 20))
    }

    @Test
    fun formatoConPuntosDeMiles() {
        assertEquals("$0", Dinero.formato(0))
        assertEquals("$500", Dinero.formato(500))
        assertEquals("$1.500", Dinero.formato(1500))
        assertEquals("$1.250.000", Dinero.formato(1_250_000))
    }
}
