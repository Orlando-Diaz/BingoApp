package com.orlando.bingoapp.ui.jugadas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orlando.bingoapp.data.BingoDatabase
import com.orlando.bingoapp.data.entity.Modo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JugadasViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = BingoDatabase.obtener(application).modoDao()

    val jugadas: StateFlow<List<Modo>> = dao.obtenerTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun eliminar(jugada: Modo) {
        viewModelScope.launch { dao.eliminar(jugada) }
    }
}
