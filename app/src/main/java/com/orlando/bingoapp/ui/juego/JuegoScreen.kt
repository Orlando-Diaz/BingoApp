package com.orlando.bingoapp.ui.juego

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orlando.bingoapp.domain.Dinero
import com.orlando.bingoapp.domain.ReglasBingo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuegoScreen(viewModel: JuegoViewModel = viewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val balotas = estado.balotas

    var balotaAQuitar by remember { mutableStateOf<Int?>(null) }
    var elegirTipo by remember { mutableStateOf(false) }
    var configurarLlena by remember { mutableStateOf(false) }
    var verificando by remember { mutableStateOf(false) }
    var sinJugadas by remember { mutableStateOf(false) }

    // El sencillo necesita al menos una jugada creada en la pestaña "Jugadas"
    val iniciarSencillo: () -> Unit = {
        if (estado.modos.isEmpty()) {
            sinJugadas = true
        } else {
            viewModel.iniciarSencillo()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Juego") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                estado.cargando -> {}
                estado.sinIniciar -> InicioPartida(
                    onSencillo = iniciarSencillo,
                    onLlena = { configurarLlena = true }
                )
                else -> {
                    TarjetaPremio(estado, onNuevaPartida = { elegirTipo = true })
                    if (estado.posiblesGanadores.isNotEmpty()) {
                        TarjetaGanadores(estado, onConfirmar = viewModel::confirmarGanadores)
                    }
                    UltimaBalota(balotas)
                    UltimasCantadas(balotas)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = viewModel::deshacerUltima,
                            enabled = balotas.isNotEmpty() && !estado.terminada,
                            modifier = Modifier.weight(1f)
                        ) { Text("Deshacer") }
                        OutlinedButton(
                            onClick = { verificando = true },
                            modifier = Modifier.weight(1f)
                        ) { Text("Verificar") }
                        Button(
                            onClick = { elegirTipo = true },
                            modifier = Modifier.weight(1f)
                        ) { Text("Nueva") }
                    }

                    TableroBalotas(
                        balotas = balotas,
                        habilitado = !estado.terminada,
                        onToque = { balota ->
                            // Si no está marcada, se marca. Si ya está, se pregunta antes de quitarla.
                            if (balota in balotas) balotaAQuitar = balota else viewModel.marcar(balota)
                        }
                    )
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

/** Cuando no hay partida en curso: elegir qué se va a jugar. */
@Composable
private fun InicioPartida(onSencillo: () -> Unit, onLlena: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("¿Qué se va a jugar?", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onSencillo, modifier = Modifier.fillMaxWidth()) { Text("Sencillo", fontSize = 18.sp) }
            Button(onClick = onLlena, modifier = Modifier.fillMaxWidth()) { Text("Llena (con premios)", fontSize = 18.sp) }
        }
    }
}

/** Qué premio está en juego, o el resumen si la partida terminó. */
@Composable
private fun TarjetaPremio(estado: EstadoJuego, onNuevaPartida: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val premio = estado.premioActual
            if (premio == null) {
                Text("Partida terminada", style = MaterialTheme.typography.titleLarge)
                estado.premios.forEach { p ->
                    Text(
                        "${p.nombre} (${Dinero.formato(estado.valorPremio(p))}): ${p.ganadores.orEmpty()} — con ${p.balotasAlGanar ?: 0} balotas",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Button(onClick = onNuevaPartida, modifier = Modifier.fillMaxWidth()) { Text("Nueva partida") }
            } else {
                val titulo = if (estado.esLlena) {
                    "Llena · premio ${premio.orden} de ${estado.premios.size}: ${premio.nombre} (${premio.porcentaje}%)"
                } else "Sencillo"
                Text(titulo, style = MaterialTheme.typography.titleLarge)
                if (!estado.esLlena) {
                    Text("Vale: " + estado.nombresModos(premio).joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                }
                // En la llena, mostrar los premios que ya salieron
                estado.premios.filter { it.ganado }.forEach { p ->
                    Text("✓ ${p.nombre}: ${p.ganadores.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                }
                val dinero = if (estado.esLlena) {
                    "Premio: ${Dinero.formato(estado.valorPremio(premio))} · Recaudado: ${Dinero.formato(estado.recaudo)}"
                } else "Recaudado: ${Dinero.formato(estado.recaudo)}"
                Text("$dinero · ${estado.tablasPagadas.size} tablas en juego", style = MaterialTheme.typography.bodyMedium)
                if (estado.tablasPagadas.isEmpty()) {
                    Text(
                        "Ninguna tabla pagada: registra los pagos en la pestaña Cobro para que la app revise ganadores.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/** Aviso grande cuando una o varias tablas completaron la figura del premio. */
@Composable
private fun TarjetaGanadores(estado: EstadoJuego, onConfirmar: () -> Unit) {
    val ganadores = estado.posiblesGanadores
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                if (ganadores.size == 1) "¡Posible ganador!" else "¡${ganadores.size} posibles ganadores! Se divide el premio",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            ganadores.forEach {
                Text(
                    "${it.descripcion} — ${it.nombreModo}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Text(
                "Confirma cuando el jugador cante bingo. Si nadie lo canta, sigue marcando balotas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Button(onClick = onConfirmar, modifier = Modifier.fillMaxWidth()) {
                Text(if (estado.esLlena && estado.premioActual?.orden != estado.premios.size) "Confirmar y seguir jugando" else "Confirmar ganador")
            }
        }
    }
}

/** La última balota cantada, bien grande. */
@Composable
private fun UltimaBalota(balotas: List<Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val ultima = balotas.lastOrNull()
            if (ultima == null) {
                Text("Toca la primera balota", style = MaterialTheme.typography.titleLarge)
            } else {
                Text(
                    "${ReglasBingo.letraDe(ultima)}-$ultima",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text("${balotas.size} de 75 cantadas", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Las 5 anteriores a la última, de la más reciente a la más vieja. */
@Composable
private fun UltimasCantadas(balotas: List<Int>) {
    val anteriores = balotas.dropLast(1).takeLast(5).reversed()
    if (anteriores.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Antes:", style = MaterialTheme.typography.bodyMedium)
        anteriores.forEach {
            Text(
                "${ReglasBingo.letraDe(it)}-$it",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Tablero de 75 balotas en 5 columnas (B I N G O), como el tablero
 * luminoso de un bingo. Las marcadas se pintan; la última se resalta.
 */
@Composable
private fun TableroBalotas(balotas: List<Int>, habilitado: Boolean, onToque: (Int) -> Unit) {
    val marcadas = balotas.toSet()
    val ultima = balotas.lastOrNull()
    val colores = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ReglasBingo.LETRAS.forEachIndexed { columna, letra ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(letra, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colores.primary)
                ReglasBingo.balotasDeColumna(columna).forEach { balota ->
                    val (fondo, texto) = when (balota) {
                        ultima -> colores.tertiary to colores.onTertiary
                        in marcadas -> colores.primary to colores.onPrimary
                        else -> colores.surfaceVariant to colores.onSurfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .background(fondo, RoundedCornerShape(10.dp))
                            .clickable(enabled = habilitado) { onToque(balota) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$balota", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = texto)
                    }
                }
            }
        }
    }
}
