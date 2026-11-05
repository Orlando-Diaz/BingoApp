package com.orlando.bingoapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Una tabla que se pagó para una partida. Si existe la fila, la tabla juega;
 * si no, no juega. La llave primaria es compuesta (partida + tabla),
 * así una tabla no se puede pagar dos veces en la misma partida.
 */
@Entity(
    tableName = "tablas_pagadas",
    primaryKeys = ["partidaId", "tablaId"],
    foreignKeys = [
        ForeignKey(
            entity = Partida::class,
            parentColumns = ["id"],
            childColumns = ["partidaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tabla::class,
            parentColumns = ["id"],
            childColumns = ["tablaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tablaId")]
)
data class TablaPagada(val partidaId: Long, val tablaId: Long)
