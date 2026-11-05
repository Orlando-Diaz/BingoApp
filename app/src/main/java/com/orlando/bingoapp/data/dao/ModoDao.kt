package com.orlando.bingoapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.orlando.bingoapp.data.entity.Modo
import kotlinx.coroutines.flow.Flow

@Dao
interface ModoDao {

    @Insert
    suspend fun insertar(modo: Modo): Long

    @Insert
    suspend fun insertarTodos(modos: List<Modo>)

    @Update
    suspend fun actualizar(modo: Modo)

    @Query("SELECT * FROM modos ORDER BY id")
    fun obtenerTodos(): Flow<List<Modo>>

    @Query("SELECT * FROM modos ORDER BY id")
    suspend fun obtenerLista(): List<Modo>

    @Query("SELECT COUNT(*) FROM modos")
    suspend fun contar(): Int

    @Delete
    suspend fun eliminar(modo: Modo)
}
