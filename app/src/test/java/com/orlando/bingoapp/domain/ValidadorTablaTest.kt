package com.orlando.bingoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorTablaTest {

    /** Tabla válida: fila f, columna c -> c*15 + f + 1 (centro libre). */
    private fun tablaValida(): MutableList<Int?> =
        MutableList(25) { i -> if (i == 12) null else (i % 5) * 15 + (i / 5) + 1 }

    @Test
    fun tablaValidaNoTieneErrores() {
        assertTrue(ValidadorTabla.errores(tablaValida()).isEmpty())
    }

    @Test
    fun detectaNumeroFueraDeRangoDeColumna() {
        val tabla = tablaValida()
        tabla[0] = 20 // columna B solo admite 1-15
        assertEquals("La B va de 1 a 15", ValidadorTabla.errores(tabla)[0])
    }

    @Test
    fun detectaRepetidos() {
        val tabla = tablaValida()
        tabla[5] = tabla[0] // misma columna B, mismo número
        val errores = ValidadorTabla.errores(tabla)
        assertEquals(ValidadorTabla.REPETIDO, errores[0])
        assertEquals(ValidadorTabla.REPETIDO, errores[5])
    }

    @Test
    fun casillaVaciaSoloSeMuestraAlValidarCompleto() {
        val tabla = tablaValida()
        tabla[3] = null
        assertEquals(ValidadorTabla.VACIA, ValidadorTabla.errores(tabla)[3])
        assertTrue(ValidadorTabla.erroresVisibles(tabla).isEmpty())
    }

    @Test
    fun ordenDeLlenadoVaPorColumnasYSaltaElCentro() {
        val orden = ReglasBingo.ORDEN_LLENADO
        assertEquals(24, orden.size)
        assertEquals(listOf(0, 5, 10, 15, 20), orden.take(5)) // toda la columna B
        assertTrue(12 !in orden)
    }

    @Test
    fun letraDeCadaBalota() {
        assertEquals("B", ReglasBingo.letraDe(1))
        assertEquals("B", ReglasBingo.letraDe(15))
        assertEquals("I", ReglasBingo.letraDe(16))
        assertEquals("G", ReglasBingo.letraDe(52))
        assertEquals("O", ReglasBingo.letraDe(75))
    }
}
