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
import com.orlando.bingoapp.data.entity.TableroConTablas
import com.orlando.bingoapp.data.entity.TableroEnPartida
import com.orlando.bingoapp.domain.Dinero
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Un tablero de la lista de cobro, con si ya pagó esta ronda. */
data class FilaCobro(val tablero: TableroConTablas, val pagado: Boolean) {
    val numero: Int get() = tablero.tablero.numero
    val id: Long get() = tablero.tablero.id
    val cantidadTablas: Int get() = tablero.tablero.cantidadTablas
}

data class EstadoCobro(
    val cargando: Boolean = true,
    val partida: Partida? = null,
    val esLlena: Boolean = false,
    val hayPremios: Boolean = false,
    val tableros: List<TableroConTablas> = emptyList(),
    val filas: List<FilaCobro> = emptyList()
) {
    val precioCuatro: Int get() = partida?.precioCuatroTablas ?: 0
    val pagados: Int get() = filas.count { it.pagado }
    val pendientes: Int get() = filas.count { !it.pagado }
    val recaudo: Int get() = filas.filter { it.pagado }.sumOf { valor(it) }

    fun valor(fila: FilaCobro): Int = Dinero.valorTablas(fila.cantidadTablas, precioCuatro)
    fun tablero(numero: Int): TableroConTablas? = tableros.find { it.tablero.numero == numero }
    fun enLista(numero: Int): Boolean = filas.any { it.numero == numero }
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
        val porId = tableros.associateBy { it.tablero.id }
        EstadoCobro(
            cargando = false,
            partida = actual?.partida,
            esLlena = (actual?.premios?.size ?: 0) > 1,
            hayPremios = !actual?.premios.isNullOrEmpty(),
            tableros = tableros,
            filas = lista.mapNotNull { r -> porId[r.tableroId]?.let { FilaCobro(it, r.pagado) } }
                .sortedBy { it.numero }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoCobro())

    var numeroTexto by mutableStateOf("")
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set

    fun cambiarNumero(texto: String) {
        numeroTexto = texto.filter { it.isDigit() }.take(4)
        mensaje = null
    }

    /** Agrega el tablero escrito a la lista, como pagado (se agrega cuando se vende). */
    fun agregar() {
        val numero = numeroTexto.toIntOrNull() ?: return
        val e = estado.value
        val partidaId = e.partida?.id ?: return
        val tablero = e.tablero(numero)
        mensaje = when {
            tablero == null -> "No hay ningún tablero con el número $numero"
            e.enLista(numero) -> "El tablero $numero ya está en la lista"
            else -> null
        }
        if (tablero == null || mensaje != null) return
        viewModelScope.launch {
            listaDao.guardar(TableroEnPartida(partidaId, tablero.tablero.id, pagado = true))
            numeroTexto = ""
        }
    }

    fun alternarPago(fila: FilaCobro) {
        val partidaId = estado.value.partida?.id ?: return
        viewModelScope.launch { listaDao.cambiarPago(partidaId, fila.id, !fila.pagado) }
    }

    fun todosPagaron() {
        val partidaId = estado.value.partida?.id ?: return
        viewModelScope.launch { listaDao.marcarTodosPagados(partidaId) }
    }

    fun quitar(fila: FilaCobro) {
        val partidaId = estado.value.partida?.id ?: return
        viewModelScope.launch { listaDao.quitar(partidaId, fila.id) }
    }

    fun vaciarLista() {
        val partidaId = estado.value.partida?.id ?: return
        viewModelScope.launch { listaDao.vaciar(partidaId) }
    }

    /**
     * Cambia un tablero de la lista por otro (ej: el 2 por el 5).
     * Devuelve un mensaje de error, o null si se pudo.
     */
    fun cambiar(fila: FilaCobro, numeroNuevo: Int): String? {
        val e = estado.value
        val partidaId = e.partida?.id ?: return "No hay una ronda en curso"
        val nuevo = e.tablero(numeroNuevo)
        return when {
            nuevo == null -> "No hay ningún tablero con el número $numeroNuevo"
            e.enLista(numeroNuevo) -> "El tablero $numeroNuevo ya lo está jugando alguien"
            else -> {
                viewModelScope.launch { listaDao.cambiar(partidaId, fila.id, nuevo.tablero.id, fila.pagado) }
                null
            }
        }
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
