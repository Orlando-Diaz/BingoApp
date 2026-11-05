package com.orlando.bingoapp.ui.jugadas

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orlando.bingoapp.data.BingoDatabase
import com.orlando.bingoapp.data.entity.Modo
import com.orlando.bingoapp.domain.Figuras
import kotlinx.coroutines.launch

/** Estado del formulario para crear o editar una jugada. */
class EditorJugadaViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = BingoDatabase.obtener(application).modoDao()

    private var id: Long = 0 // 0 = jugada nueva

    var nombre by mutableStateOf("")
        private set
    var cualquierPosicion by mutableStateOf(false)
        private set
    val figuras = mutableStateListOf(Figuras.VACIA)
    var mensajeError by mutableStateOf<String?>(null)
        private set
    var guardado by mutableStateOf(false)
        private set

    val esNueva: Boolean get() = id == 0L

    /** Cuántas figuras distintas se van a revisar realmente en el juego. */
    val posicionesTotales: Int
        get() = Figuras.expandir(figuras.filter { Figuras.cantidadCasillas(it) > 0 }, cualquierPosicion).size

    /** Prepara el formulario: vacío para una nueva, o con los datos de una existente. */
    fun cargar(jugada: Modo?) {
        id = jugada?.id ?: 0
        nombre = jugada?.nombre.orEmpty()
        cualquierPosicion = jugada?.cualquierPosicion ?: false
        figuras.clear()
        figuras.addAll(jugada?.figuras ?: listOf(Figuras.VACIA))
        mensajeError = null
        guardado = false
    }

    fun cambiarNombre(texto: String) {
        nombre = texto.take(40)
        mensajeError = null
    }

    fun cambiarCualquierPosicion(valor: Boolean) {
        cualquierPosicion = valor
    }

    fun alternar(figura: Int, casilla: Int) {
        figuras[figura] = alternarCasilla(figuras[figura], casilla)
        mensajeError = null
    }

    fun agregarFigura() {
        figuras.add(Figuras.VACIA)
    }

    fun quitarFigura(indice: Int) {
        if (figuras.size > 1) figuras.removeAt(indice)
    }

    fun guardar() {
        val dibujadas = figuras.filter { Figuras.cantidadCasillas(it) > 0 }
        mensajeError = when {
            nombre.isBlank() -> "Ponle un nombre a la jugada"
            dibujadas.isEmpty() -> "Dibuja la figura tocando las casillas"
            else -> null
        }
        if (mensajeError != null) return

        val jugada = Modo(
            id = id,
            nombre = nombre.trim(),
            figuras = dibujadas,
            cualquierPosicion = cualquierPosicion
        )
        viewModelScope.launch {
            if (esNueva) dao.insertar(jugada) else dao.actualizar(jugada)
            guardado = true
        }
    }
}
