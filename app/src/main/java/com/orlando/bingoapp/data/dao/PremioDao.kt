package com.orlando.bingoapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import com.orlando.bingoapp.data.entity.Premio

@Dao
interface PremioDao {

    @Insert
    suspend fun insertarTodos(premios: List<Premio>)

    @Update
    suspend fun actualizar(premio: Premio)
}
