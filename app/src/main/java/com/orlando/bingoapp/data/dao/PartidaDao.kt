package com.orlando.bingoapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.orlando.bingoapp.data.entity.Partida
import kotlinx.coroutines.flow.Flow

@Dao
interface PartidaDao {

    @Insert
    suspend fun insertar(partida: Partida): Long

    @Update
    suspend fun actualizar(partida: Partida)

    /** La partida en curso, "viva" para la pantalla. */
    @Query("SELECT * FROM partidas WHERE finalizada = 0 ORDER BY id DESC LIMIT 1")
    fun observarActual(): Flow<Partida?>

    @Query("SELECT * FROM partidas WHERE finalizada = 0 ORDER BY id DESC LIMIT 1")
    suspend fun obtenerActual(): Partida?
}
