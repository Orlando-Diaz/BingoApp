package com.orlando.bingoapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Un tablero que está en la lista de una partida (ronda).
 * - Si está en la lista, alguien lo tiene para jugar.
 * - pagado: si ya pagó esta ronda. Solo los pagados pueden ganar.
 *
 * Al empezar una ronda nueva, la lista se copia de la anterior con pagado = false,
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
