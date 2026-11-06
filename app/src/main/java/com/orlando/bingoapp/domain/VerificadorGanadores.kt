package com.orlando.bingoapp.domain

/** Una tabla tal como se usa para verificar (sin depender de Room). */
data class TablaEnJuego(val tableroNumero: Int, val posicion: Int, val numeros: List<Int>)

/** Un modo con sus figuras (sin depender de Room). */
data class ModoEnJuego(val nombre: String, val figuras: List<String>)

data class Ganador(
    val tableroNumero: Int,
    val posicion: Int,
    val nombreModo: String,
    val figura: String
) {
    val descripcion: String get() = "Tablero $tableroNumero · tabla $posicion"
}

/**
 * El corazón de la app: cruza las balotas cantadas con las tablas registradas
 * y dice cuáles ya completaron alguna figura del premio en juego.
 */
object VerificadorGanadores {

    /** Índices de la tabla que ya están marcados. La LIBRE del centro nunca se marca. */
    fun casillasMarcadas(numeros: List<Int>, balotas: Set<Int>): Set<Int> =
        numeros.indices
            .filter { it != ReglasBingo.INDICE_CENTRO && numeros[it] in balotas }
            .toSet()

    /**
     * La figura está completa si todas sus casillas con número están marcadas.
     * El centro se ignora: no ayuda a ganar, pero tampoco se exige.
     */
    fun figuraCompleta(figura: String, marcadas: Set<Int>): Boolean {
        val requeridas = figura.indices.filter { it != ReglasBingo.INDICE_CENTRO && figura[it] == '1' }
        return requeridas.isNotEmpty() && requeridas.all { it in marcadas }
    }

    /**
     * Devuelve una entrada por cada tabla ganadora (con la primera figura que completó).
     * Si hay varias, es un empate y se divide el premio.
     */
    fun buscarGanadores(
        tablas: List<TablaEnJuego>,
        modos: List<ModoEnJuego>,
        balotas: Set<Int>
    ): List<Ganador> = tablas.mapNotNull { tabla ->
        val marcadas = casillasMarcadas(tabla.numeros, balotas)
        modos.firstNotNullOfOrNull { modo ->
            modo.figuras.firstOrNull { figuraCompleta(it, marcadas) }?.let { figura ->
                Ganador(tabla.tableroNumero, tabla.posicion, modo.nombre, figura)
            }
        }
    }
}
