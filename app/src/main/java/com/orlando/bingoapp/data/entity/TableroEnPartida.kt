package com.orlando.bingoapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Un tablero que está jugando en una partida (ronda): en Cobro se toca y queda seleccionado.
 * Solo los que juegan (pagado = true) pueden ganar.
 *
 * Al empezar una ronda nueva, se copian los que estaban jugando en la anterior,
 * porque casi siempre siguen jugando los mismos.
 */
@Entity(
    tableName = "tableros_en_partida",
    primaryKeys = ["partidaId", "tableroId"],
    foreignKeys = [
        ForeignKey(
            entity = Partida::class,
            parentColumns = ["id"],
            childColumns = ["partidaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tablero::class,
            parentColumns = ["id"],
            childColumns = ["tableroId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tableroId")]
)
data class TableroEnPartida(
    val partidaId: Long,
    val tableroId: Long,
    val pagado: Boolean
)
