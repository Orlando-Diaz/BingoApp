package com.orlando.bingoapp.ui.juego

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orlando.bingoapp.data.Ajustes
import com.orlando.bingoapp.data.BingoDatabase
import com.orlando.bingoapp.data.entity.Modo
import com.orlando.bingoapp.data.entity.Partida
import com.orlando.bingoapp.data.entity.Premio
import com.orlando.bingoapp.domain.Dinero
import com.orlando.bingoapp.domain.Figuras
import com.orlando.bingoapp.domain.Ganador
import com.orlando.bingoapp.domain.SorteoBalotas
import com.orlando.bingoapp.domain.ModoEnJuego
import com.orlando.bingoapp.domain.TablaEnJuego
import com.orlando.bingoapp.domain.VerificadorGanadores
import com.orlando.bingoapp.voz.CantorBalotas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Todo lo que la pantalla de juego necesita mostrar. */
data class EstadoJuego(
    val cargando: Boolean = true,
    val partida: Partida? = null,
    val premios: List<Premio> = emptyList(),
    val modos: List<Modo> = emptyList(),
    val posiblesGanadores: List<Ganador> = emptyList(),
    val tablerosPagados: Set<Long> = emptySet(), // ids de los tableros que pagaron esta ronda
    val recaudo: Int = 0
) {
    val balotas: List<Int> get() = partida?.balotas.orEmpty()
    val premioActual: Premio? get() = premios.firstOrNull { !it.ganado }
    val sinIniciar: Boolean get() = premios.isEmpty()
    val terminada: Boolean get() = premios.isNotEmpty() && premioActual == null
    val esLlena: Boolean get() = premios.size > 1

    /** Los que completaron justo con la última balota: los ganadores normales. */
    val conLaUltima: List<Ganador> get() = posiblesGanadores.filter { it.balotasAlCompletar == balotas.size }

    /** En el sencillo valen todas las jugadas; en la llena, la figura de cada premio. */
    fun nombresModos(premio: Premio): List<String> =
        if (premio.figuras.isEmpty()) modos.map { it.nombre } else listOf(premio.nombre)
}

/**
 * Un premio intermedio de la llena: la figura se dibuja al iniciar la partida.
 * El valor del premio lo decide tu papá; la app no lo calcula.
 */
data class PremioIntermedio(val nombre: String, val figura: String)

data class TablaVerificada(val posicion: Int, val numeros: List<Int>, val ganadora: Ganador?)
data class Verificacion(
    val numero: Int,
    val encontrado: Boolean,
    val pagado: Boolean = false,
    val tablas: List<TablaVerificada> = emptyList()
)

class JuegoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BingoDatabase.obtener(application)
    private val partidaDao = db.partidaDao()
    private val premioDao = db.premioDao()
    private val modoDao = db.modoDao()
    private val tableroDao = db.tableroDao()
    private val listaDao = db.tableroEnPartidaDao()
    private val ajustes = Ajustes(application)

    /** Evita que dos toques muy rápidos se pisen (como synchronized, pero para corrutinas). */
    private val candado = Mutex()

    /**
     * combine junta cuatro fuentes "vivas": la partida, los tableros, las jugadas y la lista de cobro.
     * Cada vez que cualquiera cambia (por ejemplo, se marca una balota),
     * se recalcula quién va ganando. Se hace en segundo plano (Dispatchers.Default).
     */
    val estado: StateFlow<EstadoJuego> = combine(
        partidaDao.observarActualConPremios(),
        tableroDao.obtenerTodos(),
        modoDao.obtenerTodos(),
        listaDao.observarDePartidaActual()
    ) { actual, tableros, modos, lista ->
        val partida = actual?.partida
        val pagados = lista.filter { it.pagado }.map { it.tableroId }.toSet()
        val precio = partida?.precioCuatroTablas ?: 0
        val recaudo = tableros
            .filter { it.tablero.id in pagados }
            .sumOf { Dinero.valorTablas(it.tablero.cantidadTablas, precio) }
        val premios = actual?.premios.orEmpty().sortedBy { it.orden }
        val premioActual = premios.firstOrNull { !it.ganado }

        val ganadores = if (partida != null && premioActual != null) {
            // Solo juegan los tableros que pagaron esta ronda
            val tablas = tableros.filter { it.tablero.id in pagados }.flatMap { t ->
                t.tablas.map { TablaEnJuego(t.tablero.numero, it.posicion, it.numeros) }
            }
            VerificadorGanadores.buscarConMomento(
                tablas,
                modosDelPremio(premioActual, modos),
                partida.balotas
            )
        } else emptyList()

        EstadoJuego(cargando = false, partida, premios, modos, ganadores, pagados, recaudo)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoJuego())

    /** Resultado de "Verificar tablero" (null = diálogo cerrado). */
    var verificacion by mutableStateOf<Verificacion?>(null)
        private set

    // ---------- Modo automático: la app saca, marca y canta las balotas ----------

    private val cantor = CantorBalotas(application, ajustes.vozNombre, ajustes.tonoVoz)
    private var trabajoAutomatico: Job? = null

    var automatico by mutableStateOf(false)
        private set
    var segundosRestantes by mutableStateOf(0)
        private set
    var segundosEntreBalotas by mutableStateOf(ajustes.segundosEntreBalotas)
        private set
    var vozActiva by mutableStateOf(ajustes.vozActiva)
        private set
    var vozElegida by mutableStateOf(ajustes.vozNombre)
        private set
    var tonoVoz by mutableStateOf(ajustes.tonoVoz)
        private set

    /** null mientras arranca la voz; false si el celular no tiene voz en español. */
    val vozDisponible: Boolean? get() = cantor.disponible

    /**
     * Inicia (o reanuda) el modo automático. Es un ciclo en una corrutina:
     * sacar balota -> cantarla -> esperar los segundos configurados -> repetir.
     * Pausar = cancelar la corrutina. No se detiene sola con un posible ganador:
     * si nadie canta bingo, la ronda sigue y queda registrado quién la dejó pasar.
     */
    fun iniciarAutomatico() {
        if (trabajoAutomatico?.isActive == true) return
        automatico = true
        trabajoAutomatico = viewModelScope.launch {
            try {
                while (isActive) {
                    if (estado.value.premioActual == null) break // la ronda terminó
                    val balota = sacarBalota() ?: break // ya salieron las 75
                    if (vozActiva) cantor.decir(SorteoBalotas.textoParaCantar(balota))
                    for (segundo in segundosEntreBalotas downTo 1) {
                        segundosRestantes = segundo
                        delay(1_000)
                    }
                }
            } finally {
                automatico = false
                segundosRestantes = 0
            }
        }
    }

    fun pausarAutomatico() {
        trabajoAutomatico?.cancel()
        cantor.callar()
    }

    fun repetirUltima() {
        val ultima = estado.value.balotas.lastOrNull() ?: return
        viewModelScope.launch { cantor.decir(SorteoBalotas.textoParaCantar(ultima)) }
    }

    fun cambiarSegundos(segundos: Int) {
        segundosEntreBalotas = segundos
        ajustes.segundosEntreBalotas = segundos
    }

    fun voces() = cantor.vocesEnEspanol()

    /** Elige una voz y canta una balota de prueba para escucharla. */
    fun elegirVoz(nombre: String) {
        vozElegida = nombre
        ajustes.vozNombre = nombre
        cantor.usarVoz(nombre)
        probarVoz()
    }

    fun cambiarTono(tono: Float) {
        tonoVoz = tono
        cantor.cambiarTono(tono)
    }

    /** Se llama al soltar la barra del tono: lo guarda y canta una prueba. */
    fun guardarTono() {
        ajustes.tonoVoz = tonoVoz
        probarVoz()
    }

    fun probarVoz() {
        viewModelScope.launch { cantor.decir(SorteoBalotas.textoParaCantar(7)) }
    }

    fun cambiarVoz(activa: Boolean) {
        vozActiva = activa
        ajustes.vozActiva = activa
        if (!activa) cantor.callar()
    }

    /**
     * Saca una balota que no haya salido y la guarda, todo dentro del candado,
     * leyendo la partida directo de la base de datos para nunca repetir una balota.
     */
    private suspend fun sacarBalota(): Int? = candado.withLock {
        val actual = partidaDao.obtenerActual() ?: return@withLock null
        val balota = SorteoBalotas.siguiente(actual.balotas.toSet()) ?: return@withLock null
        partidaDao.actualizar(actual.copy(balotas = actual.balotas + balota))
        balota
    }

    override fun onCleared() {
        cantor.cerrar()
    }

    // ---------- Balotas ----------

    fun marcar(balota: Int) = modificarBalotas { if (balota in it) it else it + balota }

    fun quitar(balota: Int) = modificarBalotas { it - balota }

    fun deshacerUltima() = modificarBalotas { it.dropLast(1) }

    private fun modificarBalotas(cambio: (List<Int>) -> List<Int>) {
        viewModelScope.launch {
            candado.withLock {
                val actual = partidaDao.obtenerActual() ?: return@withLock
                partidaDao.actualizar(actual.copy(balotas = cambio(actual.balotas)))
            }
        }
    }

    // ---------- Partidas ----------

    fun iniciarSencillo() {
        pausarAutomatico()
        viewModelScope.launch {
            candado.withLock {
                // Sin figuras propias: el sencillo usa las jugadas de la pestaña "Jugadas"
                crearPartida(
                    listOf(Premio(orden = 1, nombre = "Sencillo", porcentaje = 100)),
                    ajustes.precioCuatroSencillo
                )
            }
        }
    }

    fun iniciarLlena(intermedios: List<PremioIntermedio>) {
        pausarAutomatico()
        viewModelScope.launch {
            candado.withLock {
                val premios = intermedios.mapIndexed { i, p ->
                    Premio(orden = i + 1, nombre = p.nombre, figuras = listOf(p.figura), porcentaje = 0)
                } + Premio(
                    orden = intermedios.size + 1,
                    nombre = Figuras.NOMBRE_LLENO,
                    figuras = Figuras.LLENO,
                    porcentaje = 0
                )
                crearPartida(premios, ajustes.precioCuatroLlena)
            }
        }
    }

    /** Cierra la partida actual (si hay) y crea una nueva con sus premios. */
    /**
     * Cierra la ronda actual y crea la nueva con sus premios.
     * La lista de tableros pasa a la nueva ronda como pendiente de pago.
     */
    private suspend fun crearPartida(premios: List<Premio>, precioCuatroTablas: Int) {
        val anterior = partidaDao.obtenerActual()
        anterior?.let { partidaDao.actualizar(it.copy(finalizada = true)) }
        val partidaId = partidaDao.insertar(Partida(precioCuatroTablas = precioCuatroTablas))
        premioDao.insertarTodos(premios.map { it.copy(partidaId = partidaId) })
        val idAnterior = anterior?.id ?: partidaDao.ultimaFinalizadaId()
        idAnterior?.let { listaDao.copiarLista(anterior = it, nueva = partidaId) }
    }

    /**
     * Marca como ganado el premio en juego con los ganadores que eligió tu papá
     * (por defecto, los que completaron con la última balota). Los demás que ya
     * habían completado quedan registrados como "la dejaron pasar".
     * En la llena, el juego sigue con el siguiente premio y las mismas balotas.
     */
    fun confirmarGanadores(elegidos: List<Ganador>) {
        if (elegidos.isEmpty()) return
        viewModelScope.launch {
            candado.withLock {
                val e = estado.value
                val premio = e.premioActual ?: return@withLock
                val sePasaron = e.posiblesGanadores.filter { it !in elegidos }
                premioDao.actualizar(
                    premio.copy(
                        ganado = true,
                        balotasAlGanar = e.balotas.size,
                        ganadores = elegidos.joinToString(", ") { it.descripcion },
                        dejaronPasar = sePasaron
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString(", ") { "${it.descripcion} (completó con la balota ${it.balotasAlCompletar})" }
                    )
                )
            }
        }
    }

    // ---------- Verificar un tablero ----------

    fun verificarTablero(numero: Int) {
        viewModelScope.launch {
            val e = estado.value
            val tablero = tableroDao.buscarPorNumero(numero)
            val premio = e.premioActual
            val modos = if (premio != null) modosDelPremio(premio, e.modos) else emptyList()
            val balotas = e.balotas.toSet()

            verificacion = if (tablero == null) {
                Verificacion(numero, encontrado = false)
            } else {
                // Un tablero que no pagó esta ronda no puede ganar
                val pagado = tablero.tablero.id in e.tablerosPagados
                Verificacion(numero, encontrado = true, pagado = pagado, tablas = tablero.tablas.sortedBy { it.posicion }.map { tabla ->
                    val ganadora = if (!pagado) null else VerificadorGanadores.buscarGanadores(
                        listOf(TablaEnJuego(numero, tabla.posicion, tabla.numeros)), modos, balotas
                    ).firstOrNull()
                    TablaVerificada(tabla.posicion, tabla.numeros, ganadora)
                })
            }
        }
    }

    fun cerrarVerificacion() {
        verificacion = null
    }

    /**
     * Qué figuras se revisan para un premio:
     * - Llena: las figuras guardadas en el premio.
     * - Sencillo: todas las jugadas, expandidas si valen en cualquier posición.
     */
    private fun modosDelPremio(premio: Premio, modos: List<Modo>): List<ModoEnJuego> =
        if (premio.figuras.isNotEmpty()) {
            listOf(ModoEnJuego(premio.nombre, premio.figuras))
        } else {
            modos.map { ModoEnJuego(it.nombre, Figuras.expandir(it.figuras, it.cualquierPosicion)) }
        }
}
