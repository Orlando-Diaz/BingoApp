package com.orlando.bingoapp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Un modo de juego (cartón lleno, letra L, línea, 4 esquinas...).
 *
 * figuras: cada figura es un texto de 25 caracteres '1'/'0', fila por fila.
 * '1' = la casilla hay que completarla. Un modo puede tener varias figuras
 * (por ejemplo "Línea horizontal" tiene 5) y se gana completando CUALQUIERA de ellas.
 *
 * usarEnSencillo: si este modo vale para ganar en las partidas sencillas.
 */
@Entity(tableName = "modos")
data class Modo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val figuras: List<String>,
    @ColumnInfo(defaultValue = "0") val usarEnSencillo: Boolean = false
)
