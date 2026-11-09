package com.orlando.bingoapp.domain

/** Una tabla tal como se usa para verificar (sin depender de Room). */
data class TablaEnJuego(val tableroNumero: Int, val posicion: Int, val numeros: List<Int>)

/** Un modo con sus figuras (sin depender de Room). */
data class ModoEnJuego(val nombre: String, val figuras: List<String>)

data class Ganador(
    val tableroNumero: Int,
    val posicion: Int,
    val nombreModo: String,
    val figura: String,
    /** Con cuántas balotas cantadas completó la figura (ej: 23 = con la balota número 23). */
    val balotasAlCompletar: Int = 0
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
    /**
     * Como buscarGanadores, pero además dice CON QUÉ BALOTA completó cada tabla.
     * Sirve para saber quién la dejó pasar: si completó antes de la última balota
     * y nadie cantó bingo, se le pasó.
     *
     * @param balotasEnOrden las balotas en el orden en que salieron.
     */
    fun buscarConMomento(
        tablas: List<TablaEnJuego>,
        modos: List<ModoEnJuego>,
        balotasEnOrden: List<Int>
    ): List<Ganador> {
        // Para cada número: en qué turno salió (1 = la primera balota)
        val turno = balotasEnOrden.withIndex().associate { it.value to it.index + 1 }
        return tablas.mapNotNull { tabla ->
            modos.flatMap { modo ->
                modo.figuras.mapNotNull { figura ->
                    val casillas = figura.indices.filter { it != ReglasBingo.INDICE_CENTRO && figura[it] == '1' }
                    if (casillas.isEmpty()) return@mapNotNull null
                    val turnos = casillas.map { turno[tabla.numeros[it]] ?: return@mapNotNull null }
                    // La figura se completó cuando salió el último número que le faltaba
                    Ganador(tabla.tableroNumero, tabla.posicion, modo.nombre, figura, balotasAlCompletar = turnos.max())
                }
            }.minByOrNull { it.balotasAlCompletar } // la primera figura que completó
        }
    }

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
