package com.orlando.bingoapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.orlando.bingoapp.data.entity.TablaPagada
import kotlinx.coroutines.flow.Flow

@Dao
interface TablaPagadaDao {

    /** IGNORE: si ya estaba pagada, no pasa nada (no lanza error). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarTodas(pagos: List<TablaPagada>)

    @Query("DELETE FROM tablas_pagadas WHERE partidaId = :partidaId AND tablaId IN (:tablaIds)")
    suspend fun quitar(partidaId: Long, tablaIds: List<Long>)

    /** Pagos de la partida en curso, "vivos" para las pantallas. */
    @Query(
        "SELECT tp.* FROM tablas_pagadas tp " +
            "INNER JOIN partidas p ON p.id = tp.partidaId " +
            "WHERE p.finalizada = 0"
    )
    fun observarDePartidaActual(): Flow<List<TablaPagada>>

    /** Copia los pagos de una partida a otra (INSERT ... SELECT en una sola consulta). */
    @Query(
        "INSERT OR IGNORE INTO tablas_pagadas (partidaId, tablaId) " +
            "SELECT :nueva, tablaId FROM tablas_pagadas WHERE partidaId = :anterior"
    )
    suspend fun copiar(anterior: Long, nueva: Long)
}
