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
    val VACIA = "0".repeat(25)

    /** Casillas que hay que completar (la LIBRE del centro nunca cuenta). */
    fun cantidadCasillas(figura: String): Int =
        figura.indices.count { it != ReglasBingo.INDICE_CENTRO && figura[it] == '1' }

    /**
     * Todas las posiciones donde cabe la figura moviéndola por la tabla.
     * Ej: un cuadrito 2x2 dibujado arriba a la izquierda -> los 16 cuadritos posibles.
     *     una fila completa -> las 5 filas.
     */
    fun traslaciones(figura: String): List<String> {
        val celdas = figura.indices.filter { figura[it] == '1' }.map { it / 5 to it % 5 }
        if (celdas.isEmpty()) return listOf(figura)

        val minFila = celdas.minOf { it.first }
        val maxFila = celdas.maxOf { it.first }
        val minCol = celdas.minOf { it.second }
        val maxCol = celdas.maxOf { it.second }

        return (-minFila..(4 - maxFila)).flatMap { df ->
            (-minCol..(4 - maxCol)).map { dc ->
                conIndices(celdas.map { (f, c) -> (f + df) * 5 + (c + dc) })
            }
        }.distinct()
    }

    fun usaCentro(figura: String): Boolean = figura[ReglasBingo.INDICE_CENTRO] == '1'

    /**
     * Las figuras que realmente se revisan al verificar una jugada.
     *
     * La casilla LIBRE del centro nunca sirve para ganar, así que al mover una figura
     * por la tabla se descartan las posiciones que caen encima de ella:
     * cuadrito de 4 -> 12 posiciones (no 16), línea horizontal -> 4 (no 5).
     * Si la figura dibujada ya pasaba por el centro (ej: una X), el centro solo se ignora.
     */
    fun expandir(figuras: List<String>, cualquierPosicion: Boolean): List<String> {
        if (!cualquierPosicion) return figuras
        return figuras.flatMap { original ->
            val posiciones = traslaciones(original)
            if (usaCentro(original)) posiciones else posiciones.filterNot { usaCentro(it) }
        }.distinct()
    }
}
