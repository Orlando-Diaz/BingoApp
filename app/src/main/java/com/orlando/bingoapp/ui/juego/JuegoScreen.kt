package com.orlando.bingoapp.ui.juego

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import com.orlando.bingoapp.domain.Dinero
import com.orlando.bingoapp.domain.Ganador
import com.orlando.bingoapp.domain.ReglasBingo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuegoScreen(
    onIrACobro: () -> Unit,
    viewModel: JuegoViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val balotas = estado.balotas

    var balotaAQuitar by remember { mutableStateOf<Int?>(null) }
    var elegirTipo by remember { mutableStateOf(false) }
    var configurarLlena by remember { mutableStateOf(false) }
    var verificando by remember { mutableStateOf(false) }
    var sinJugadas by remember { mutableStateOf(false) }

    // Flujo de ronda nueva: elegir tipo -> crear la ronda -> ir a Cobro
    val iniciarSencillo: () -> Unit = {
        if (estado.modos.isEmpty()) {
            sinJugadas = true // el sencillo necesita jugadas creadas en la pestaña "Jugadas"
        } else {
            viewModel.iniciarSencillo()
            onIrACobro()
        }
    }

    // Mientras la app canta sola, la pantalla no se apaga
    val vista = LocalView.current
    DisposableEffect(viewModel.automatico) {
        vista.keepScreenOn = viewModel.automatico
        onDispose { vista.keepScreenOn = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tituloRonda(estado), maxLines = 1) },
                actions = {
                    if (!estado.sinIniciar) {
                        TextButton(onClick = { verificando = true }) { Text("Verificar") }
                        TextButton(onClick = { elegirTipo = true }) { Text("Nueva") }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                estado.cargando -> {}
                estado.sinIniciar -> InicioPartida(
                    onSencillo = iniciarSencillo,
                    onLlena = { configurarLlena = true }
                )
                else -> {
                    BarraBalotas(
                        balotas = balotas,
                        puedeDeshacer = balotas.isNotEmpty() && !estado.terminada && !viewModel.automatico,
                        onDeshacer = viewModel::deshacerUltima
                    )
                    if (!estado.terminada) {
                        ControlAutomatico(viewModel, hayBalotas = balotas.isNotEmpty())
                    }
                    if (estado.posiblesGanadores.isNotEmpty()) {
                        TarjetaGanadores(estado, onConfirmar = viewModel::confirmarGanadores)
                    }
                    if (estado.tablerosPagados.isEmpty()) {
                        AvisoSinPagos(onIrACobro)
                    }
                    if (estado.terminada) {
                        TarjetaTerminada(estado, onNueva = { elegirTipo = true })
                    }

                    PanelBalotas(
                        balotas = balotas,
                        habilitado = !estado.terminada,
                        onToque = { balota ->
                            // Si no está marcada, se marca. Si ya está, se pregunta antes de quitarla.
                            if (balota in balotas) balotaAQuitar = balota else viewModel.marcar(balota)
                        }
                    )

                    if (!estado.terminada) InfoRonda(estado)
                    Box(Modifier.height(8.dp))
                }
            }
        }
    }

    balotaAQuitar?.let { balota ->
        AlertDialog(
            onDismissRequest = { balotaAQuitar = null },
            title = { Text("¿Quitar la ${ReglasBingo.letraDe(balota)}-$balota?") },
            text = { Text("Úsalo solo si la marcaste por error.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.quitar(balota)
                    balotaAQuitar = null
                }) { Text("Quitar") }
            },
            dismissButton = { TextButton(onClick = { balotaAQuitar = null }) { Text("Cancelar") } }
        )
    }

    if (elegirTipo) {
        DialogoNuevaPartida(
            onSencillo = {
                elegirTipo = false
                iniciarSencillo()
            },
            onLlena = {
                elegirTipo = false
                configurarLlena = true
            },
            onCancelar = { elegirTipo = false }
        )
    }

    if (configurarLlena) {
        DialogoConfigurarLlena(
            onIniciar = { intermedios ->
                viewModel.iniciarLlena(intermedios)
                configurarLlena = false
                onIrACobro()
            },
            onCancelar = { configurarLlena = false }
        )
    }

    if (sinJugadas) {
        AlertDialog(
            onDismissRequest = { sinJugadas = false },
            title = { Text("No hay jugadas para los sencillos") },
            text = { Text("Primero agrega las formas de ganar en la pestaña \"Jugadas\".") },
            confirmButton = { TextButton(onClick = { sinJugadas = false }) { Text("Entendido") } }
        )
    }

    if (verificando) {
        DialogoVerificarTablero(
            verificacion = viewModel.verificacion,
            balotas = balotas.toSet(),
            onBuscar = viewModel::verificarTablero,
            onCerrar = {
                verificando = false
                viewModel.cerrarVerificacion()
            }
        )
    }
}

private fun tituloRonda(estado: EstadoJuego): String {
    val premio = estado.premioActual
    return when {
        estado.sinIniciar -> "Juego"
        premio == null -> "Ronda terminada"
        estado.esLlena -> "Llena · ${premio.nombre} (${premio.orden}/${estado.premios.size})"
        else -> "Sencillo"
    }
}

/** Cuando no hay ronda en curso: elegir qué se va a jugar. */
@Composable
private fun InicioPartida(onSencillo: () -> Unit, onLlena: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("¿Qué se va a jugar?", style = MaterialTheme.typography.headlineSmall)
            Text("Después de elegir, pasas a Cobro para ver quién pagó.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onSencillo, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Sencillo", fontSize = 18.sp) }
            Button(onClick = onLlena, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Llena (con premios)", fontSize = 18.sp) }
        }
    }
}

/**
 * Franja compacta de arriba: la última balota en un círculo grande,
 * cuántas van y las anteriores. Así el panel de 75 queda visible.
 */
@Composable
private fun BarraBalotas(balotas: List<Int>, puedeDeshacer: Boolean, onDeshacer: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val ultima = balotas.lastOrNull()
        Box(
            modifier = Modifier.size(92.dp).background(colores.tertiary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (ultima == null) "–" else ReglasBingo.letraDe(ultima),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colores.onTertiary
                )
                Text(
                    ultima?.toString() ?: "",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = colores.onTertiary
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (ultima == null) "Toca la primera balota" else "${balotas.size} de 75 cantadas",
                style = MaterialTheme.typography.titleMedium
            )
            val anteriores = balotas.dropLast(1).takeLast(4).reversed()
            if (anteriores.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    anteriores.forEach {
                        Text(
                            "${ReglasBingo.letraDe(it)}$it",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier
                                .background(colores.secondaryContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            OutlinedButton(onClick = onDeshacer, enabled = puedeDeshacer) { Text("↶ Deshacer última") }
        }
    }
}

/**
 * El panel de 75 balotas en columnas B-I-N-G-O, como el tablero luminoso de un bingo.
 * Las cantadas quedan pintadas y tachadas; la última, resaltada con borde.
 */
@Composable
private fun PanelBalotas(balotas: List<Int>, habilitado: Boolean, onToque: (Int) -> Unit) {
    val marcadas = balotas.toSet()
    val ultima = balotas.lastOrNull()
    val colores = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ReglasBingo.LETRAS.forEachIndexed { columna, letra ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(40.dp).background(colores.primary, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(letra, fontSize = 24.sp, fontWeight = FontWeight.Black, color = colores.onPrimary)
                }
                ReglasBingo.balotasDeColumna(columna).forEach { balota ->
                    val esUltima = balota == ultima
                    val marcada = balota in marcadas
                    val forma = RoundedCornerShape(10.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(
                                when {
                                    esUltima -> colores.tertiary
                                    marcada -> colores.primaryContainer
                                    else -> colores.surface
                                },
                                forma
                            )
                            .border(
                                width = if (esUltima) 3.dp else 1.dp,
                                color = if (esUltima) colores.onSurface else colores.outlineVariant,
                                shape = forma
                            )
                            .clickable(enabled = habilitado) { onToque(balota) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$balota",
                            fontSize = 24.sp,
                            fontWeight = if (marcada) FontWeight.Black else FontWeight.Medium,
                            textDecoration = if (marcada && !esUltima) TextDecoration.LineThrough else null,
                            color = when {
                                esUltima -> colores.onTertiary
                                marcada -> colores.onPrimaryContainer
                                else -> colores.onSurface.copy(alpha = 0.55f)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvisoSinPagos(onIrACobro: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Ningún tablero ha pagado: la app no puede revisar ganadores.",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            TextButton(onClick = onIrACobro) { Text("Ir a Cobro") }
        }
    }
}

/** Datos de la ronda: qué vale para ganar, cuánto se recogió y premios ya entregados. */
@Composable
private fun InfoRonda(estado: EstadoJuego) {
    val premio = estado.premioActual ?: return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (estado.esLlena) {
                Text("Premio en juego: ${premio.nombre} · ${Dinero.formato(estado.valorPremio(premio))}", style = MaterialTheme.typography.titleSmall)
                estado.premios.filter { it.ganado }.forEach { p ->
                    Text("✓ ${p.nombre}: ${p.ganadores.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                    p.dejaronPasar?.let {
                        Text("La dejaron pasar: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Text("Vale: " + estado.nombresModos(premio).joinToString(", "), style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Recaudado: ${Dinero.formato(estado.recaudo)} · ${estado.tablerosPagados.size} tableros en juego",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun TarjetaTerminada(estado: EstadoJuego, onNueva: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Ronda terminada", style = MaterialTheme.typography.titleLarge)
            estado.premios.forEach { p ->
                Text(
                    "${p.nombre} (${Dinero.formato(estado.valorPremio(p))}): ${p.ganadores.orEmpty()} — con ${p.balotasAlGanar ?: 0} balotas",
                    style = MaterialTheme.typography.bodyMedium
                )
                p.dejaronPasar?.let {
                    Text("La dejaron pasar: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Button(onClick = onNueva, modifier = Modifier.fillMaxWidth()) { Text("Nueva ronda") }
        }
    }
}

/**
 * Aviso cuando una o varias tablas completaron la figura del premio.
 * Por defecto quedan elegidas las que completaron con la última balota;
 * las que completaron antes "la dejaron pasar", pero tu papá puede incluirlas.
 */
@Composable
private fun TarjetaGanadores(estado: EstadoJuego, onConfirmar: (List<Ganador>) -> Unit) {
    val posibles = estado.posiblesGanadores
    val elegidos = remember(posibles) {
        mutableStateMapOf<Ganador, Boolean>().apply { posibles.forEach { put(it, it in estado.conLaUltima) } }
    }
    val cantidad = elegidos.count { it.value }
    val colores = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colores.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                if (estado.conLaUltima.isNotEmpty()) "¡Posible ganador!" else "Completaron, pero nadie cantó bingo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colores.onTertiaryContainer
            )
            posibles.forEach { g ->
                val conUltima = g.balotasAlCompletar == estado.balotas.size
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = elegidos[g] == true, onCheckedChange = { elegidos[g] = it })
                    Column {
                        Text("${g.descripcion} — ${g.nombreModo}", style = MaterialTheme.typography.titleMedium, color = colores.onTertiaryContainer)
                        Text(
                            if (conUltima) "Completó con la última balota"
                            else "Completó con la balota ${g.balotasAlCompletar}: la dejó pasar",
                            style = MaterialTheme.typography.bodySmall,
                            color = colores.onTertiaryContainer
                        )
                    }
                }
            }
            if (cantidad > 1) {
                Text("Se divide el premio entre $cantidad", style = MaterialTheme.typography.bodyMedium, color = colores.onTertiaryContainer)
            }
            Button(
                onClick = { onConfirmar(posibles.filter { elegidos[it] == true }) },
                enabled = cantidad > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (estado.esLlena && estado.premioActual?.orden != estado.premios.size) "Confirmar y seguir jugando" else "Confirmar ganador")
            }
        }
    }
}

/** Controles del modo automático: la app saca, marca y canta las balotas sola. */
@Composable
private fun ControlAutomatico(viewModel: JuegoViewModel, hayBalotas: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (viewModel.automatico) {
                    Button(onClick = viewModel::pausarAutomatico, modifier = Modifier.weight(1f).height(52.dp)) {
                        Text("⏸  Pausar", fontSize = 18.sp)
                    }
                } else {
                    Button(onClick = viewModel::iniciarAutomatico, modifier = Modifier.weight(1f).height(52.dp)) {
                        Text(if (hayBalotas) "▶  Reanudar" else "▶  Cantar solo", fontSize = 18.sp)
                    }
                }
                OutlinedButton(onClick = viewModel::repetirUltima, enabled = hayBalotas, modifier = Modifier.height(52.dp)) {
                    Text("🔊 Repetir")
                }
            }
            if (viewModel.automatico && viewModel.segundosRestantes > 0) {
                Text("Siguiente balota en ${viewModel.segundosRestantes} s", style = MaterialTheme.typography.titleSmall)
            }
            Text("Una balota cada ${viewModel.segundosEntreBalotas} segundos", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = viewModel.segundosEntreBalotas.toFloat(),
                onValueChange = { viewModel.cambiarSegundos(it.roundToInt()) },
                valueRange = 3f..30f,
                steps = 26 // un paso por cada segundo entre 3 y 30
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cantar en voz alta", modifier = Modifier.weight(1f))
                Switch(checked = viewModel.vozActiva, onCheckedChange = viewModel::cambiarVoz)
            }
            if (viewModel.vozActiva && viewModel.vozDisponible == false) {
                Text(
                    "Este celular no tiene voz en español. Instala \"Servicios de voz de Google\" desde Play Store.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
