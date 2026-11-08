package com.orlando.bingoapp.ui.tableros

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orlando.bingoapp.data.BingoDatabase
import com.orlando.bingoapp.data.entity.TableroConTablas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Muestra un tablero con todas sus tablas y permite cambiarle el número o eliminarlo. */
class DetalleTableroViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = BingoDatabase.obtener(application).tableroDao()
    private val tableroId = MutableStateFlow(0L)

    /**
     * flatMapLatest: cada vez que cambia el id (se abre otro tablero),
     * deja de escuchar el anterior y empieza a escuchar el nuevo.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val tablero: StateFlow<TableroConTablas?> = tableroId
        .flatMapLatest { dao.observarPorId(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun cargar(id: Long) {
        tableroId.value = id
    }

    /** Devuelve un mensaje de error, o null si se pudo cambiar. */
    suspend fun cambiarNumero(nuevo: Int): String? {
        val actual = tablero.value?.tablero ?: return "No se encontró el tablero"
        if (nuevo <= 0) return "Escribe un número válido"
        if (nuevo == actual.numero) return null
        if (dao.buscarPorNumero(nuevo) != null) return "Ya existe un tablero con el número $nuevo"
        dao.actualizarTablero(actual.copy(numero = nuevo))
        return null
    }

    fun eliminar(alTerminar: () -> Unit) {
        val actual = tablero.value?.tablero ?: return
        viewModelScope.launch {
            dao.eliminar(actual)
            alTerminar()
        }
    }
}
