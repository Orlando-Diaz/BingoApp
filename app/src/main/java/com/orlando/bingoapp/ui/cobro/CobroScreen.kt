package com.orlando.bingoapp.ui.cobro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.RadioButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orlando.bingoapp.data.entity.Tablero
import com.orlando.bingoapp.domain.Dinero

/**
 * Cobro de la ronda como un tablero de números: aparecen TODOS los tableros
 * registrados y se toca cada uno que va a jugar. En la ronda siguiente quedan
 * seleccionados los mismos; "Limpiar" los quita todos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CobroScreen(
    onEmpezar: () -> Unit,
    viewModel: CobroViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var editandoPrecios by remember { mutableStateOf(false) }
    var ajustandoVista by remember { mutableStateOf(false) }
    val tamano = TamanoCasilla.entries[viewModel.tamano.coerceIn(0, 2)]
    val medidas = tamano.medidas(esTablet())
    var limpiando by remember { mutableStateOf(false) }
    val enRonda = estado.partida != null && estado.hayPremios

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (!enRonda) "Cobro" else if (estado.esLlena) "Cobro · Llena" else "Cobro · Sencillo") },
                actions = {
                    TextButton(onClick = { ajustandoVista = true }) { Text("👓 Vista") }
                    TextButton(onClick = { editandoPrecios = true }) { Text("Precios") }
                }
            )
        },
        bottomBar = {
            if (enRonda) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = onEmpezar,
                        enabled = estado.jugando.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().padding(12.dp).height(56.dp)
                    ) {
                        Text(
                            if (estado.jugando.isNotEmpty()) "Empezar a jugar · ${estado.jugando.size} tableros"
                            else "Toca los tableros que van a jugar",
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        when {
            estado.cargando -> {}
            !enRonda -> Text(
                "Primero toca \"Nueva\" en la pestaña Juego y elige sencillo o llena; " +
                    "la app te trae aquí para cobrar.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(padding).padding(24.dp)
            )
            estado.tableros.isEmpty() -> Text(
                "No hay tableros registrados. Regístralos en la pestaña Tableros.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(padding).padding(24.dp)
            )
            else -> LazyVerticalGrid(
                // Columnas fijas según el tamaño elegido y si es celular o tablet
                columns = GridCells.Fixed(medidas.columnas),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // El resumen ocupa toda la fila (span = todas las columnas)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Resumen(estado, onLimpiar = { limpiando = true })
                }
                items(estado.tableros, key = { it.id }) { tablero ->
                    CasillaTablero(
                        tablero = tablero,
                        jugando = tablero.id in estado.jugando,
                        medidas = medidas,
                        onToque = { viewModel.alternar(tablero) }
                    )
                }
            }
        }
    }

    if (ajustandoVista) {
        DialogoVista(
            tamano = viewModel.tamano,
            onTamano = viewModel::cambiarTamano,
            onCerrar = { ajustandoVista = false }
        )
    }

    if (editandoPrecios) {
        DialogoPrecios(
            sencilloInicial = viewModel.precioCuatroSencillo,
            llenaInicial = viewModel.precioCuatroLlena,
            onGuardar = { sencillo, llena ->
                viewModel.guardarPrecios(sencillo, llena)
                editandoPrecios = false
            },
            onCancelar = { editandoPrecios = false }
        )
    }

    if (limpiando) {
        AlertDialog(
            onDismissRequest = { limpiando = false },
            title = { Text("¿Limpiar la selección?") },
            text = { Text("Se quitan todos los tableros seleccionados. Úsalo al empezar una noche nueva.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.limpiar()
                    limpiando = false
                }) { Text("Limpiar") }
            },
            dismissButton = { TextButton(onClick = { limpiando = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun Resumen(estado: EstadoCobro, onLimpiar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "Recaudado: ${Dinero.formato(estado.recaudo)}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "${estado.jugando.size} de ${estado.tableros.size} tableros jugando",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Tablero de 4: ${Dinero.formato(estado.precioCuatro)} · de 8: ${Dinero.formato(estado.precioCuatro * 2)}",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Toca los tableros que juegan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = onLimpiar, enabled = estado.jugando.isNotEmpty()) { Text("Limpiar") }
            }
        }
    }
}

/** Cuántos tableros caben por fila y el tamaño de la letra. */
private data class Medidas(val columnas: Int, val numero: TextUnit, val detalle: TextUnit)

/**
 * Tamaños de las casillas para ver mejor. En la tablet caben más por fila,
 * porque la pantalla es mucho más ancha que la de un celular.
 */
private enum class TamanoCasilla(val nombre: String, val celular: Medidas, val tablet: Medidas) {
    PEQUENO("Pequeño", Medidas(5, 22.sp, 10.sp), Medidas(7, 30.sp, 13.sp)),
    GRANDE("Grande", Medidas(4, 30.sp, 12.sp), Medidas(6, 38.sp, 15.sp)),
    MUY_GRANDE("Muy grande", Medidas(3, 44.sp, 15.sp), Medidas(5, 50.sp, 18.sp));

    fun medidas(esTablet: Boolean): Medidas = if (esTablet) tablet else celular
}

/**
 * Es tablet si su lado más corto mide 600 dp o más (el criterio que usa Android).
 * Se usa el lado más corto para que no cambie al girar la pantalla.
 */
@Composable
private fun esTablet(): Boolean = LocalConfiguration.current.smallestScreenWidthDp >= 600

/**
 * Colores fijos de alto contraste (no dependen del tema del celular):
 * jugando = verde oscuro con número blanco; libre = blanco con borde negro grueso.
 */
private val VerdeJugando = Color(0xFF1B5E20)
private val NegroBorde = Color(0xFF111111)

/** Un tablero en la cuadrícula. Al tocarlo vibra un poco para confirmar el toque. */
@Composable
private fun CasillaTablero(tablero: Tablero, jugando: Boolean, medidas: Medidas, onToque: () -> Unit) {
    val vibracion = LocalHapticFeedback.current
    val forma = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(if (jugando) VerdeJugando else Color.White, forma)
            .border(if (jugando) 0.dp else 3.dp, NegroBorde, forma)
            .clickable {
                vibracion.performHapticFeedback(HapticFeedbackType.LongPress)
                onToque()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${tablero.numero}",
                fontSize = medidas.numero,
                fontWeight = FontWeight.Black,
                color = if (jugando) Color.White else NegroBorde
            )
            Text(
                if (jugando) "✓ JUEGA" else "${tablero.cantidadTablas} tablas",
                fontSize = medidas.detalle,
                fontWeight = FontWeight.Bold,
                color = if (jugando) Color.White else NegroBorde
            )
        }
    }
}

/** Opciones para ver mejor el cobro: tamaño de las casillas. */
@Composable
private fun DialogoVista(
    tamano: Int,
    onTamano: (Int) -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Vista del cobro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tamaño de los números", style = MaterialTheme.typography.titleMedium)
                val tablet = esTablet()
                TamanoCasilla.entries.forEachIndexed { i, opcion ->
                    val medidasOpcion = opcion.medidas(tablet)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onTamano(i) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = tamano == i, onClick = { onTamano(i) })
                        Text(
                            "${opcion.nombre} (${medidasOpcion.columnas} por fila)",
                            fontSize = medidasOpcion.detalle * 1.3f
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Listo") } }
    )
}

@Composable
private fun DialogoPrecios(
    sencilloInicial: Int,
    llenaInicial: Int,
    onGuardar: (Int, Int) -> Unit,
    onCancelar: () -> Unit
) {
    var sencillo by remember { mutableStateOf(sencilloInicial.toString()) }
    var llena by remember { mutableStateOf(llenaInicial.toString()) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Precios") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Precio de un tablero de 4 tablas. El de 8 vale el doble.")
                OutlinedTextField(
                    value = sencillo,
                    onValueChange = { sencillo = it.filter(Char::isDigit).take(7) },
                    label = { Text("Sencillo (4 tablas)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = llena,
                    onValueChange = { llena = it.filter(Char::isDigit).take(7) },
                    label = { Text("Llena (4 tablas)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Text(
                    "Los precios nuevos aplican desde la próxima partida.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGuardar(sencillo.toIntOrNull() ?: 0, llena.toIntOrNull() ?: 0) },
                enabled = (sencillo.toIntOrNull() ?: 0) > 0 && (llena.toIntOrNull() ?: 0) > 0
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
