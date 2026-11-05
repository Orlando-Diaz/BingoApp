package com.orlando.bingoapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Una tabla de bingo 5x5 pegada en un tablero.
 *
 * numeros: los 25 números leídos fila por fila (de izquierda a derecha,
 * de arriba hacia abajo). El centro (índice 12) es la casilla libre y se guarda como 0.
 */
@Entity(
    tableName = "tablas",
    foreignKeys = [
        ForeignKey(
            entity = Tablero::class,
            parentColumns = ["id"],
            childColumns = ["tableroId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tableroId", "posicion"], unique = true)]
)
data class Tabla(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableroId: Long = 0,
    val posicion: Int, // 1 a 8 dentro del tablero
    val numeros: List<Int>,
    val fotoPath: String? = null // ruta de la foto, cuando se registre con cámara
) {
    companion object {
        const val CENTRO_LIBRE = 0
        const val INDICE_CENTRO = 12
    }
}
