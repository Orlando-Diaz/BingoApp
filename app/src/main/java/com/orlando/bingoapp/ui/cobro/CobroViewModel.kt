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
import com.orlando.bingoapp.data.entity.Tabla
import com.orlando.bingoapp.data.entity.TablaPagada
import com.orlando.bingoapp.data.entity.TableroConTablas
import com.orlando.bingoapp.domain.Dinero
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
    val tableros: List<TableroConTablas> = emptyList(),
    val pagadas: Set<Long> = emptySet()
) {
    val precioCuatro: Int get() = partida?.precioCuatroTablas ?: 0
    val recaudo: Int get() = Dinero.valorTablas(pagadas.size, precioCuatro)

    /** Tableros que tienen al menos una tabla pagada, con cuántas. */
    val tablerosConPago: List<Pair<TableroConTablas, Int>>
        get() = tableros.mapNotNull { t ->
            val cantidad = t.tablas.count { it.id in pagadas }
            if (cantidad > 0) t to cantidad else null
        }

    fun tablero(numero: Int): TableroConTablas? = tableros.find { it.tablero.numero == numero }
}

class CobroViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BingoDatabase.obtener(application)
    private val partidaDao = db.partidaDao()
    private val pagoDao = db.tablaPagadaDao()
    private val ajustes = Ajustes(application)

    val estado: StateFlow<EstadoCobro> = combine(
        partidaDao.observarActualConPremios(),
        db.tableroDao().obtenerTodos(),
        pagoDao.observarDePartidaActual()
    ) { actual, tableros, pagos ->
        EstadoCobro(
            cargando = false,
            partida = actual?.partida,
            esLlena = (actual?.premios?.size ?: 0) > 1,
            hayPremios = !actual?.premios.isNullOrEmpty(),
            tableros = tableros,
            pagadas = pagos.map { it.tablaId }.toSet()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoCobro())

    var numeroTexto by mutableStateOf("")
        private set
    var tableroSeleccionado by mutableStateOf<Int?>(null)
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set

    fun cambiarNumero(texto: String) {
        numeroTexto = texto.filter { it.isDigit() }.take(4)
        mensaje = null
    }

    fun buscar() {
        val numero = numeroTexto.toIntOrNull() ?: return
        if (estado.value.tablero(numero) == null) {
            mensaje = "No hay ningún tablero con el número $numero"
            tableroSeleccionado = null
        } else {
            tableroSeleccionado = numero
            mensaje = null
        }
    }

    fun seleccionar(numero: Int) {
        tableroSeleccionado = numero
        numeroTexto = numero.toString()
        mensaje = null
    }

    /** Cierra el tablero y deja el campo listo para el siguiente. */
    fun listo() {
        tableroSeleccionado = null
        numeroTexto = ""
    }

    /** Toque en una tabla: si estaba pagada deja de jugar, y al revés. */
    fun alternar(tabla: Tabla) {
        val partidaId = estado.value.partida?.id ?: return
        viewModelScope.launch {
            if (tabla.id in estado.value.pagadas) pagoDao.quitar(partidaId, listOf(tabla.id))
            else pagoDao.insertarTodas(listOf(TablaPagada(partidaId, tabla.id)))
        }
    }

    /** El caso más común: pagaron todo el tablero. Un toque y listo para el siguiente. */
    fun todasPagas(tablero: TableroConTablas) {
        val partidaId = estado.value.partida?.id ?: return
        viewModelScope.launch {
            pagoDao.insertarTodas(tablero.tablas.map { TablaPagada(partidaId, it.id) })
            listo()
        }
    }

    fun ninguna(tablero: TableroConTablas) {
        val partidaId = estado.value.partida?.id ?: return
        viewModelScope.launch { pagoDao.quitar(partidaId, tablero.tablas.map { it.id }) }
    }

    fun copiarPartidaAnterior() {
        val actual = estado.value.partida ?: return
        viewModelScope.launch {
            val anterior = partidaDao.ultimaFinalizadaId()
            if (anterior == null) {
                mensaje = "No hay una partida anterior para copiar"
            } else {
                pagoDao.copiar(anterior, actual.id)
            }
        }
    }

    // ---------- Precios ----------

    val precioCuatroSencillo: Int get() = ajustes.precioCuatroSencillo
    val precioCuatroLlena: Int get() = ajustes.precioCuatroLlena

    /** Los precios nuevos aplican desde la próxima partida que se inicie. */
    fun guardarPrecios(sencillo: Int, llena: Int) {
        ajustes.precioCuatroSencillo = sencillo
        ajustes.precioCuatroLlena = llena
    }
}
