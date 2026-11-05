package com.orlando.bingoapp.ui.cobro

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orlando.bingoapp.data.entity.TableroConTablas
import com.orlando.bingoapp.domain.Dinero

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CobroScreen(viewModel: CobroViewModel = viewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var editandoPrecios by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cobro") },
                actions = { TextButton(onClick = { editandoPrecios = true }) { Text("Precios") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                estado.cargando -> {}
                estado.partida == null || !estado.hayPremios -> Text(
                    "Primero inicia una partida en la pestaña Juego y luego registra aquí quién pagó.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 24.dp)
                )
                else -> {
                    Resumen(estado)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = viewModel.numeroTexto,
                            onValueChange = viewModel::cambiarNumero,
                            label = { Text("Número del tablero") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.buscar() }),
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = viewModel::buscar, enabled = viewModel.numeroTexto.isNotEmpty()) {
                            Text("Buscar")
                        }
                    }
                    viewModel.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                    viewModel.tableroSeleccionado?.let { numero ->
                        estado.tablero(numero)?.let { tablero ->
                            TarjetaCobroTablero(tablero, estado, viewModel)
                        }
                    }

                    if (estado.pagadas.isEmpty()) {
                        OutlinedButton(
                            onClick = viewModel::copiarPartidaAnterior,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Copiar pagos de la partida anterior") }
                    }

                    ListaPagados(estado, onSeleccionar = viewModel::seleccionar)
                }
            }
        }
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
}

@Composable
private fun Resumen(estado: EstadoCobro) {
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
                "${estado.pagadas.size} tablas en juego · ${if (estado.esLlena) "Llena" else "Sencillo"}: " +
                    "4 tablas ${Dinero.formato(estado.precioCuatro)}, 8 tablas ${Dinero.formato(estado.precioCuatro * 2)}",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/** Las tablas del tablero buscado: un toque cambia entre pagada y "no juega". */
@Composable
private fun TarjetaCobroTablero(
    tablero: TableroConTablas,
    estado: EstadoCobro,
    viewModel: CobroViewModel
) {
    val tablas = tablero.tablas.sortedBy { it.posicion }
    val pagadas = tablas.count { it.id in estado.pagadas }
    val colores = MaterialTheme.colorScheme

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Tablero ${tablero.tablero.numero}", style = MaterialTheme.typography.titleLarge)
            Text(
                "$pagadas de ${tablas.size} tablas · ${Dinero.formato(Dinero.valorTablas(pagadas, estado.precioCuatro))}",
                style = MaterialTheme.typography.bodyLarge
            )

            // Filas de 4 tablas
            tablas.chunked(4).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fila.forEach { tabla ->
                        val pagada = tabla.id in estado.pagadas
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .background(
                                    if (pagada) colores.primary else colores.surfaceVariant,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.alternar(tabla) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${tabla.posicion}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pagada) colores.onPrimary else colores.onSurfaceVariant
                                )
                                Text(
                                    if (pagada) "Paga" else "No juega",
                                    fontSize = 11.sp,
                                    color = if (pagada) colores.onPrimary else colores.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Button(onClick = { viewModel.todasPagas(tablero) }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Todas pagas · ${Dinero.formato(Dinero.valorTablas(tablas.size, estado.precioCuatro))}",
                    fontSize = 18.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.ninguna(tablero) }, modifier = Modifier.weight(1f)) {
                    Text("Ninguna")
                }
                OutlinedButton(onClick = viewModel::listo, modifier = Modifier.weight(1f)) {
                    Text("Listo")
                }
            }
        }
    }
}

@Composable
private fun ListaPagados(estado: EstadoCobro, onSeleccionar: (Int) -> Unit) {
    val lista = estado.tablerosConPago
    if (lista.isEmpty()) return
    Text("Tableros que pagaron (${lista.size})", style = MaterialTheme.typography.titleMedium)
    lista.forEach { (tablero, cantidad) ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSeleccionar(tablero.tablero.numero) }
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Tablero ${tablero.tablero.numero}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text("$cantidad de ${tablero.tablas.size} · ${Dinero.formato(Dinero.valorTablas(cantidad, estado.precioCuatro))}")
            }
        }
    }
    Box(Modifier.height(16.dp))
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
