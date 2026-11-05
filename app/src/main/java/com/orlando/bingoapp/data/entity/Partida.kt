package com.orlando.bingoapp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una partida de bingo.
 * balotas: los números en el orden en que fueron cantados.
 * finalizada: true cuando se empieza una partida nueva.
 * precioCuatroTablas: lo que costaba un tablero de 4 tablas en esta partida.
 *   Se guarda aquí para que, si después cambian los precios, el historial no cambie.
 */
@Entity(tableName = "partidas")
data class Partida(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: Long = System.currentTimeMillis(),
    val balotas: List<Int> = emptyList(),
    val finalizada: Boolean = false,
    @ColumnInfo(defaultValue = "0") val precioCuatroTablas: Int = 0
)
