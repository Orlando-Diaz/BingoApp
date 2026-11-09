package com.orlando.bingoapp.domain

import kotlin.random.Random

/** Saca balotas al azar, como una balotera: nunca repite una que ya salió. */
object SorteoBalotas {

    /** Una balota al azar entre las que faltan, o null si ya salieron las 75. */
    fun siguiente(cantadas: Set<Int>, azar: Random = Random.Default): Int? {
        val faltan = (1..75).filter { it !in cantadas }
        return if (faltan.isEmpty()) null else faltan[azar.nextInt(faltan.size)]
    }

    /** Lo que dice la voz: "B. 7" se escucha como "be... siete". */
    fun textoParaCantar(balota: Int): String = "${ReglasBingo.letraDe(balota)}. $balota"
}
