package com.orlando.bingoapp.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PartidaConPremios(
    @Embedded val partida: Partida,
    @Relation(parentColumn = "id", entityColumn = "partidaId")
    val premios: List<Premio>
)
