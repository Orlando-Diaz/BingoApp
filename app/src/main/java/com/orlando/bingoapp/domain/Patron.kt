package com.orlando.bingoapp.domain

import java.security.MessageDigest

/**
 * Patrón de 3x3 puntos (numerados 0 a 8, fila por fila) para proteger la pestaña Tableros.
 * Se guarda solo una "huella" (hash SHA-256), no el patrón en sí:
 * igual que una contraseña bien guardada en una base de datos.
 */
object Patron {

    const val MINIMO_PUNTOS = 4

    fun esValido(puntos: List<Int>): Boolean =
        puntos.size >= MINIMO_PUNTOS && puntos.distinct().size == puntos.size && puntos.all { it in 0..8 }

    fun huella(puntos: List<Int>): String =
        MessageDigest.getInstance("SHA-256")
            .digest(puntos.joinToString("-").toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun coincide(puntos: List<Int>, huellaGuardada: String): Boolean = huella(puntos) == huellaGuardada
}
