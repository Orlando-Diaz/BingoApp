package com.orlando.bingoapp.ui.cobro

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orlando.bingoapp.data.Ajustes
import com.orlando.bingoapp.data.BingoDatabase
import com.orlando.bingoapp.data.entity.Partida
import com.orlando.bingoapp.data.entity.Tablero
import com.orlando.bingoapp.data.entity.TableroEnPartida
import com.orlando.bingoapp.domain.Dinero
import com.orlando.bingoapp.voz.CantorBalotas
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EstadoCobro(
    val cargando: Boolean = true,
    val partida: Partida? = null,
    val esLlena: Boolean = false,
    val hayPremios: Boolean = false,
    val tableros: List<Tablero> = emptyList(),   // todos los registrados, por número
    val jugando: Set<Long> = emptySet()          // ids de los seleccionados en esta ronda
) {
    val precioCuatro: Int get() = partida?.precioCuatroTablas ?: 0
    val recaudo: Int
        get() = tableros.filter { it.id in jugando }.sumOf { Dinero.valorTablas(it.cantidadTablas, precioCuatro) }
}

class CobroViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BingoDatabase.obtener(application)
    private val listaDao = db.tableroEnPartidaDao()
    private val ajustes = Ajustes(application)

    val estado: StateFlow<EstadoCobro> = combine(
        db.partidaDao().observarActualConPremios(),
        db.tableroDao().obtenerTodos(),
        listaDao.observarDePartidaActual()
    ) { actual, tableros, lista ->
        EstadoCobro(
            cargando = false,
            partida = actual?.partida,
            esLlena = (actual?.premios?.size ?: 0) > 1,
            hayPremios = !actual?.premios.isNullOrEmpty(),
            tableros = tableros.map { it.tablero }.sortedBy { it.numero },
            jugando = lista.filter { it.pagado }.map { it.tableroId }.toSet()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoCobro())

    // ---------- Ayudas para ver mejor ----------

    /** Misma voz y tono que se eligieron para cantar las balotas. */
    private val cantor = CantorBalotas(application, ajustes.vozNombre, ajustes.tonoVoz)

    var tamano by mutableStateOf(ajustes.tamanoCobro)
        private set
    var vozAlTocar by mutableStateOf(ajustes.vozEnCobro)
        private set

    fun cambiarTamano(valor: Int) {
        tamano = valor
        ajustes.tamanoCobro = valor
    }

    fun cambiarVozAlTocar(valor: Boolean) {
        vozAlTocar = valor
        ajustes.vozEnCobro = valor
    }

    /** Tocar un tablero: si no estaba jugando, entra; si estaba, sale. */
    fun alternar(tablero: Tablero) {
        val partidaId = estado.value.partida?.id ?: return
        val estaJugando = tablero.id in estado.value.jugando
        viewModelScope.launch {
            if (estaJugando) listaDao.quitar(partidaId, tablero.id)
            else listaDao.guardar(TableroEnPartida(partidaId, tablero.id, pagado = true))
        }
        // Confirmación de oído: así sabe que tocó el tablero correcto
        if (vozAlTocar) {
            viewModelScope.launch {
                cantor.decir(if (estaJugando) "Quitado ${tablero.numero}" else "Tablero ${tablero.numero}")
            }
        }
    }

    override fun onCleared() {
        cantor.cerrar()
    }

    /** Quita la selección de todos (por ejemplo, al empezar una noche nueva). */
    fun limpiar() {
        val partidaId = estado.value.partida?.id ?: return
        viewModelScope.launch { listaDao.vaciar(partidaId) }
    }

    // ---------- Precios ----------

    val precioCuatroSencillo: Int get() = ajustes.precioCuatroSencillo
    val precioCuatroLlena: Int get() = ajustes.precioCuatroLlena

    /** Los precios nuevos aplican desde la próxima ronda que se inicie. */
    fun guardarPrecios(sencillo: Int, llena: Int) {
        ajustes.precioCuatroSencillo = sencillo
        ajustes.precioCuatroLlena = llena
    }
}
