package com.orlando.bingoapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.orlando.bingoapp.data.entity.Tabla
import com.orlando.bingoapp.data.entity.Tablero
import com.orlando.bingoapp.data.entity.TableroConTablas
import kotlinx.coroutines.flow.Flow

@Dao
interface TableroDao {

    @Insert
    suspend fun insertarTablero(tablero: Tablero): Long

    @Insert
    suspend fun insertarTablas(tablas: List<Tabla>)

    /** Guarda el tablero y sus tablas en una sola transacción (todo o nada). */
    @Transaction
    suspend fun guardarTableroConTablas(tablero: Tablero, tablas: List<Tabla>): Long {
        val tableroId = insertarTablero(tablero)
        insertarTablas(tablas.map { it.copy(tableroId = tableroId) })
        return tableroId
    }

    /** Lista que se actualiza sola en pantalla cuando cambian los datos. */
    @Transaction
    @Query("SELECT * FROM tableros ORDER BY numero")
    fun obtenerTodos(): Flow<List<TableroConTablas>>

    @Transaction
    @Query("SELECT * FROM tableros WHERE numero = :numero")
    suspend fun buscarPorNumero(numero: Int): TableroConTablas?

    @Delete
    suspend fun eliminar(tablero: Tablero)

    // ---------- Ver y editar ----------

    @Transaction
    @Query("SELECT * FROM tableros WHERE id = :id")
    fun observarPorId(id: Long): Flow<TableroConTablas?>

    @Query("SELECT * FROM tablas WHERE id = :id")
    suspend fun obtenerTabla(id: Long): Tabla?

    @Update
    suspend fun actualizarTabla(tabla: Tabla)

    @Update
    suspend fun actualizarTablero(tablero: Tablero)
}
