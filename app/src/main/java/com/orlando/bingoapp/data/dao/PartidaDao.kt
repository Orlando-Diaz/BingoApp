package com.orlando.bingoapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.orlando.bingoapp.data.entity.Partida
import com.orlando.bingoapp.data.entity.PartidaConPremios
import kotlinx.coroutines.flow.Flow

@Dao
interface PartidaDao {

    @Insert
    suspend fun insertar(partida: Partida): Long

    @Update
    suspend fun actualizar(partida: Partida)

    /** La partida en curso con sus premios, "viva" para la pantalla. */
    @Transaction
    @Query("SELECT * FROM partidas WHERE finalizada = 0 ORDER BY id DESC LIMIT 1")
    fun observarActualConPremios(): Flow<PartidaConPremios?>

    @Query("SELECT * FROM partidas WHERE finalizada = 0 ORDER BY id DESC LIMIT 1")
    suspend fun obtenerActual(): Partida?
}
