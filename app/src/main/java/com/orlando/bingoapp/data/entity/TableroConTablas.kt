package com.orlando.bingoapp.data.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Un tablero junto con todas sus tablas.
 * Es como un @OneToMany de JPA, pero aquí se arma solo al consultar.
 */
data class TableroConTablas(
    @Embedded val tablero: Tablero,
    @Relation(parentColumn = "id", entityColumn = "tableroId")
    val tablas: List<Tabla>
)
