package com.orlando.bingoapp.domain

/** Cálculos de dinero en pesos (enteros, sin decimales). */
object Dinero {

    /**
     * Valor de un número de tablas a partir del precio de 4.
     * Ej: con 4 a $500 -> 8 tablas = $1.000, 6 tablas = $750.
     */
    fun valorTablas(cantidad: Int, precioCuatroTablas: Int): Int = cantidad * precioCuatroTablas / 4

    fun porcentaje(total: Int, porcentaje: Int): Int = total * porcentaje / 100

    /** 1250000 -> "$1.250.000" (separador de miles con punto, como en Colombia). */
    fun formato(pesos: Int): String {
        val digitos = kotlin.math.abs(pesos).toString()
        val conPuntos = digitos.reversed().chunked(3).joinToString(".").reversed()
        return (if (pesos < 0) "-$" else "$") + conPuntos
    }
}
