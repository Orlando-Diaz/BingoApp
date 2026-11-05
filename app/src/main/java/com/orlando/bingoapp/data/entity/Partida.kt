package com.orlando.bingoapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una partida de bingo.
 * balotas: los números en el orden en que fueron cantados.
 * finalizada: true cuando se empieza una partida nueva.
 */
@Entity(tableName = "partidas")
data class Partida(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: Long = System.currentTimeMillis(),
    val balotas: List<Int> = emptyList(),
    val finalizada: Boolean = false
)
