package com.orlando.bingoapp.domain

/**
 * Figuras con las que se puede ganar. Cada figura es un texto de 25 caracteres
 * ('1' = casilla que hay que completar), leído fila por fila.
 */
object Figuras {

    /**
     * Convierte un dibujo de 5 filas en una figura. Más fácil de leer que los índices:
     *   dibujo("X...X", ".....", ".....", ".....", "X...X")  -> 4 esquinas
     */
    fun dibujo(vararg filas: String): String {
        require(filas.size == 5 && filas.all { it.length == 5 }) { "El dibujo debe ser de 5x5" }
        return filas.joinToString("") { fila -> fila.map { if (it == 'X') '1' else '0' }.joinToString("") }
    }

    fun conIndices(indices: Iterable<Int>): String {
        val marcadas = indices.toSet()
        return (0 until 25).joinToString("") { if (it in marcadas) "1" else "0" }
    }

    val CUATRO_ESQUINAS = listOf(dibujo("X...X", ".....", ".....", ".....", "X...X"))

    /** Cualquier cuadrado de 2x2 en la tabla: hay 16 posibles. */
    val CUADRITOS = (0 until 4).flatMap { fila ->
        (0 until 4).map { col ->
            val i = fila * 5 + col
            conIndices(listOf(i, i + 1, i + 5, i + 6))
        }
    }

    val HORIZONTALES = (0 until 5).map { fila -> conIndices((0 until 5).map { fila * 5 + it }) }
    val VERTICALES = (0 until 5).map { col -> conIndices((0 until 5).map { it * 5 + col }) }
    val DIAGONALES = listOf(
        conIndices(listOf(0, 6, 12, 18, 24)),
        conIndices(listOf(4, 8, 12, 16, 20))
    )

    val LLENO = listOf("1".repeat(25))
    val X = listOf(dibujo("X...X", ".X.X.", "..X..", ".X.X.", "X...X"))
    val L = listOf(dibujo("X....", "X....", "X....", "X....", "XXXXX"))
    val T = listOf(dibujo("XXXXX", "..X..", "..X..", "..X..", "..X.."))
    val U = listOf(dibujo("X...X", "X...X", "X...X", "X...X", "XXXXX"))
    val MARCO = listOf(dibujo("XXXXX", "X...X", "X...X", "X...X", "XXXXX"))

    const val NOMBRE_LLENO = "Cartón lleno"
}
