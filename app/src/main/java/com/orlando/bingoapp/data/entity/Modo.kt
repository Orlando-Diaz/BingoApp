package com.orlando.bingoapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Un modo de juego (cartón lleno, letra L, línea, 4 esquinas...).
 *
 * figuras: cada figura es un texto de 25 caracteres '1'/'0', fila por fila.
 * '1' = la casilla hay que completarla. Un modo puede tener varias figuras
 * (por ejemplo "Línea" tiene 12) y se gana completando CUALQUIERA de ellas.
 */
@Entity(tableName = "modos")
data class Modo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val figuras: List<String>
)
