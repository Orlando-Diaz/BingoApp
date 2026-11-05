package com.orlando.bingoapp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una jugada para ganar en los sencillos (4 esquinas, cuadrito, línea...).
 * Las crean y editan tus papás desde la pestaña "Jugadas".
 *
 * figuras: cada figura es un texto de 25 caracteres '1'/'0', fila por fila.
 * Se gana completando CUALQUIERA de ellas.
 *
 * cualquierPosicion: si es true, cada figura vale en cualquier parte de la tabla
 * (un cuadrito dibujado una vez vale en las 16 posiciones).
 *
 * usarEnSencillo: columna de la versión 3; ya no se usa (todas las jugadas valen en el sencillo).
 */
@Entity(tableName = "modos")
data class Modo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val figuras: List<String>,
    @ColumnInfo(defaultValue = "0") val usarEnSencillo: Boolean = true,
    @ColumnInfo(defaultValue = "0") val cualquierPosicion: Boolean = false
)
