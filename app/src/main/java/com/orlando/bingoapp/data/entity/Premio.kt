package com.orlando.bingoapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Un premio dentro de una partida.
 * - Sencillo: un solo premio que acepta varios modos (4 esquinas, líneas...).
 * - Llena: premios intermedios con porcentaje y al final el cartón lleno.
 *
 * Se juegan en orden: el premio en juego es el primero que no se ha ganado.
 */
@Entity(
    tableName = "premios",
    foreignKeys = [
        ForeignKey(
            entity = Partida::class,
            parentColumns = ["id"],
            childColumns = ["partidaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("partidaId")]
)
data class Premio(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partidaId: Long = 0,
    val orden: Int,
    val nombre: String,
    val modoIds: List<Long>,
    val porcentaje: Int,
    val ganado: Boolean = false,
    val balotasAlGanar: Int? = null, // con cuántas balotas cantadas se ganó
    val ganadores: String? = null    // ej: "Tablero 12 · tabla 3, Tablero 5 · tabla 1"
)
