package com.orlando.bingoapp.ui.juego

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orlando.bingoapp.data.BingoDatabase
import com.orlando.bingoapp.data.entity.Partida
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Maneja la partida en curso. Cada balota marcada se guarda de inmediato
 * en la base de datos, así que si se cierra la app no se pierde el juego.
 */
class JuegoViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = BingoDatabase.obtener(application).partidaDao()

    /**
     * Evita que dos toques muy rápidos se pisen entre sí
     * (algo parecido a un bloque synchronized en Java, pero para corrutinas).
     */
    private val candado = Mutex()

    val partida: StateFlow<Partida?> = dao.observarActual()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Si no hay partida en curso (primera vez), se crea una
        viewModelScope.launch {
            candado.withLock {
                if (dao.obtenerActual() == null) dao.insertar(Partida())
            }
        }
    }

    fun marcar(balota: Int) = modificar { balotas ->
        if (balota in balotas) balotas else balotas + balota
    }

    fun quitar(balota: Int) = modificar { balotas -> balotas - balota }

    fun deshacerUltima() = modificar { balotas -> balotas.dropLast(1) }

    fun nuevaPartida() {
        viewModelScope.launch {
            candado.withLock {
                dao.obtenerActual()?.let { dao.actualizar(it.copy(finalizada = true)) }
                dao.insertar(Partida())
            }
        }
    }

    /** Lee la partida actual, le aplica el cambio a sus balotas y la guarda. */
    private fun modificar(cambio: (List<Int>) -> List<Int>) {
        viewModelScope.launch {
            candado.withLock {
                val actual = dao.obtenerActual() ?: return@withLock
                dao.actualizar(actual.copy(balotas = cambio(actual.balotas)))
            }
        }
    }
}
