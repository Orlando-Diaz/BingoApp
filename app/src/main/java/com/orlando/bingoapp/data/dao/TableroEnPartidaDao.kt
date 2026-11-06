package com.orlando.bingoapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.orlando.bingoapp.data.entity.TableroEnPartida
import kotlinx.coroutines.flow.Flow

@Dao
interface TableroEnPartidaDao {

    /** REPLACE: si el tablero ya estaba en la lista, se actualiza. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(registro: TableroEnPartida)

    @Query("UPDATE tableros_en_partida SET pagado = :pagado WHERE partidaId = :partidaId AND tableroId = :tableroId")
    suspend fun cambiarPago(partidaId: Long, tableroId: Long, pagado: Boolean)

    /** "Todos pagaron": un solo UPDATE para toda la lista. */
    @Query("UPDATE tableros_en_partida SET pagado = 1 WHERE partidaId = :partidaId")
    suspend fun marcarTodosPagados(partidaId: Long)

    @Query("DELETE FROM tableros_en_partida WHERE partidaId = :partidaId AND tableroId = :tableroId")
    suspend fun quitar(partidaId: Long, tableroId: Long)

    @Query("DELETE FROM tableros_en_partida WHERE partidaId = :partidaId")
    suspend fun vaciar(partidaId: Long)

    /** Cambiar un tablero por otro (ej: el 2 por el 5), conservando si ya pagó. */
    @Transaction
    suspend fun cambiar(partidaId: Long, tableroViejo: Long, tableroNuevo: Long, pagado: Boolean) {
        quitar(partidaId, tableroViejo)
        guardar(TableroEnPartida(partidaId, tableroNuevo, pagado))
    }

    /** La lista de la partida en curso, "viva" para las pantallas. */
    @Query(
        "SELECT tp.* FROM tableros_en_partida tp " +
            "INNER JOIN partidas p ON p.id = tp.partidaId " +
            "WHERE p.finalizada = 0"
    )
    fun observarDePartidaActual(): Flow<List<TableroEnPartida>>

    /** Copia la lista de una ronda a la siguiente, todos como pendientes de pago. */
    @Query(
        "INSERT OR IGNORE INTO tableros_en_partida (partidaId, tableroId, pagado) " +
            "SELECT :nueva, tableroId, 0 FROM tableros_en_partida WHERE partidaId = :anterior"
    )
    suspend fun copiarLista(anterior: Long, nueva: Long)
}
