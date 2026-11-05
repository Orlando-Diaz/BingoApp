package com.orlando.bingoapp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Un tablero físico: la lámina donde van pegadas 4 u 8 tablas.
 * El número es el que tiene pintado el tablero y no se puede repetir.
 */
@Entity(
    tableName = "tableros",
    indices = [Index(value = ["numero"], unique = true)]
)
data class Tablero(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numero: Int,
    val cantidadTablas: Int // 4 u 8
)
