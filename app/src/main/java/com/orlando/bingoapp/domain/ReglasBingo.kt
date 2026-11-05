package com.orlando.bingoapp.domain

/**
 * Reglas del bingo de 75 balotas. Es Kotlin puro (no depende de Android),
 * así que se puede probar con pruebas unitarias normales.
 */
object ReglasBingo {

    val LETRAS = listOf("B", "I", "N", "G", "O")
    const val INDICE_CENTRO = 12

    /** B 1-15, I 16-30, N 31-45, G 46-60, O 61-75 */
    fun rangoColumna(columna: Int): IntRange = (columna * 15 + 1)..(columna * 15 + 15)

    fun columnaDe(indice: Int): Int = indice % 5

    /** Letra de una balota: 7 -> "B", 52 -> "G". */
    fun letraDe(balota: Int): String = LETRAS[(balota - 1) / 15]

    /** Todas las balotas de una columna: la B tiene del 1 al 15, etc. */
    fun balotasDeColumna(columna: Int): List<Int> = rangoColumna(columna).toList()

    /**
     * Orden en que se llenan las casillas: columna por columna (toda la B, luego la I...),
     * porque así se lee una tabla de bingo. Se salta el centro libre.
     */
    val ORDEN_LLENADO: List<Int> =
        (0 until 5).flatMap { col -> (0 until 5).map { fila -> fila * 5 + col } }
            .filter { it != INDICE_CENTRO }
}

/**
 * Revisa una tabla de 25 casillas (null = vacía) y devuelve los errores por índice.
 */
object ValidadorTabla {

    const val VACIA = "Vacía"
    const val REPETIDO = "Repetido"

    fun errores(numeros: List<Int?>): Map<Int, String> {
        require(numeros.size == 25) { "Una tabla tiene 25 casillas" }
        val errores = mutableMapOf<Int, String>()

        numeros.forEachIndexed { indice, numero ->
            if (indice == ReglasBingo.INDICE_CENTRO) return@forEachIndexed
            val columna = ReglasBingo.columnaDe(indice)
            val rango = ReglasBingo.rangoColumna(columna)
            when {
                numero == null -> errores[indice] = VACIA
                numero !in rango ->
                    errores[indice] = "La ${ReglasBingo.LETRAS[columna]} va de ${rango.first} a ${rango.last}"
            }
        }

        // Números repetidos dentro de la misma tabla
        numeros.withIndex()
            .filter { it.value != null && it.index != ReglasBingo.INDICE_CENTRO }
            .groupBy { it.value }
            .values
            .filter { it.size > 1 }
            .flatten()
            .forEach { errores.putIfAbsent(it.index, REPETIDO) }

        return errores
    }

    /** Errores que se muestran mientras se escribe (las vacías no se marcan todavía). */
    fun erroresVisibles(numeros: List<Int?>): Map<Int, String> =
        errores(numeros).filterValues { it != VACIA }
}
