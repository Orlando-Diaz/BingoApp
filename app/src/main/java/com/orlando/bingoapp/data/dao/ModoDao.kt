package com.orlando.bingoapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.orlando.bingoapp.data.entity.Modo
import kotlinx.coroutines.flow.Flow

@Dao
interface ModoDao {

    @Insert
    suspend fun insertar(modo: Modo): Long

    @Insert
    suspend fun insertarTodos(modos: List<Modo>)

    @Query("SELECT * FROM modos ORDER BY nombre")
    fun obtenerTodos(): Flow<List<Modo>>

    @Query("SELECT COUNT(*) FROM modos")
    suspend fun contar(): Int

    @Delete
    suspend fun eliminar(modo: Modo)
}
