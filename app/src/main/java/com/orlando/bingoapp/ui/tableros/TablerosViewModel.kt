package com.orlando.bingoapp.ui.tableros

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orlando.bingoapp.data.BingoDatabase
import com.orlando.bingoapp.data.entity.Tablero
import com.orlando.bingoapp.data.entity.TableroConTablas
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel de la lista de tableros (algo así como un @Service para esta pantalla). */
class TablerosViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = BingoDatabase.obtener(application).tableroDao()

    val tableros: StateFlow<List<TableroConTablas>> = dao.obtenerTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun eliminar(tablero: Tablero) {
        viewModelScope.launch { dao.eliminar(tablero) }
    }
}
