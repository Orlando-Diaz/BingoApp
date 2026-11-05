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

    /** Índices de la tabla que ya están marcados (el centro libre siempre cuenta). */
    fun casillasMarcadas(numeros: List<Int>, balotas: Set<Int>): Set<Int> =
        numeros.indices
            .filter { it == ReglasBingo.INDICE_CENTRO || numeros[it] in balotas }
            .toSet()

    fun figuraCompleta(figura: String, marcadas: Set<Int>): Boolean =
        figura.withIndex().all { (indice, c) -> c != '1' || indice in marcadas }

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
