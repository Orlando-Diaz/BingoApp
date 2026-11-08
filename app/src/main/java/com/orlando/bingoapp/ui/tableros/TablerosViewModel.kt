package com.orlando.bingoapp.ui.tableros

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orlando.bingoapp.data.BingoDatabase
import com.orlando.bingoapp.data.CopiaSeguridad
import com.orlando.bingoapp.data.entity.Tablero
import com.orlando.bingoapp.data.entity.TableroConTablas
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/** Estado de la copia de seguridad que ve la pantalla. */
sealed interface EstadoCopia {
    data object Nada : EstadoCopia
    data object Trabajando : EstadoCopia
    data class ListaParaCompartir(val archivo: File) : EstadoCopia
    data object Restaurada : EstadoCopia
    data class Error(val mensaje: String) : EstadoCopia
}

/** ViewModel de la lista de tableros (algo así como un @Service para esta pantalla). */
class TablerosViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = BingoDatabase.obtener(application).tableroDao()
    private val copia = CopiaSeguridad(application)

    val tableros: StateFlow<List<TableroConTablas>> = dao.obtenerTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var estadoCopia by mutableStateOf<EstadoCopia>(EstadoCopia.Nada)
        private set

    fun eliminar(tablero: Tablero) {
        viewModelScope.launch { dao.eliminar(tablero) }
    }

    fun exportar() {
        viewModelScope.launch {
            estadoCopia = EstadoCopia.Trabajando
            estadoCopia = try {
                EstadoCopia.ListaParaCompartir(copia.exportar())
            } catch (e: Exception) {
                EstadoCopia.Error("No se pudo crear la copia")
            }
        }
    }

    fun restaurar(uri: Uri) {
        viewModelScope.launch {
            estadoCopia = EstadoCopia.Trabajando
            val error = copia.prepararRestauracion(uri)
            estadoCopia = if (error == null) EstadoCopia.Restaurada else EstadoCopia.Error(error)
        }
    }

    fun limpiarCopia() {
        estadoCopia = EstadoCopia.Nada
    }
}
