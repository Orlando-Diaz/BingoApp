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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orlando.bingoapp.domain.Dinero

/**
 * Cobro de la ronda: la lista de tableros que están jugando (se copia de la ronda anterior)
 * y si cada uno ya pagó. Al terminar, "Empezar a jugar" lleva a la pantalla de juego.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CobroScreen(
    onEmpezar: () -> Unit,
    viewModel: CobroViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var editandoPrecios by remember { mutableStateOf(false) }
    var cambiando by remember { mutableStateOf<FilaCobro?>(null) }
    var quitando by remember { mutableStateOf<FilaCobro?>(null) }
    var vaciando by remember { mutableStateOf(false) }
    val enRonda = estado.partida != null && estado.hayPremios

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (!enRonda) "Cobro" else if (estado.esLlena) "Cobro · Llena" else "Cobro · Sencillo") },
                actions = { TextButton(onClick = { editandoPrecios = true }) { Text("Precios") } }
            )
        },
        bottomBar = {
            if (enRonda) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = onEmpezar,
                        enabled = estado.pagados > 0,
                        modifier = Modifier.fillMaxWidth().padding(12.dp).height(56.dp)
                    ) {
                        Text(
                            if (estado.pagados > 0) "Empezar a jugar · ${estado.pagados} tableros" else "Nadie ha pagado todavía",
                            fontSize = 18.sp
                        )
                    }
                }
            }
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
                !enRonda -> Text(
                    "Primero toca \"Nueva\" en la pestaña Juego y elige sencillo o llena; " +
                        "la app te trae aquí para cobrar.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 24.dp)
                )
                else -> {
                    Resumen(estado)
                    AgregarTablero(viewModel)

                    if (estado.pendientes > 0) {
                        Button(onClick = viewModel::todosPagaron, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Text("✓ Todos pagaron (${estado.pendientes} pendientes)", fontSize = 17.sp)
                        }
                    }

                    if (estado.filas.isEmpty()) {
                        Text(
                            "La lista está vacía. Escribe el número de cada tablero que vendas y toca Agregar.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        Text("Toca \"Pagó / Debe\" para cambiarlo", style = MaterialTheme.typography.bodySmall)
                        estado.filas.forEach { fila ->
                            FilaTablero(
                                fila = fila,
                                valor = estado.valor(fila),
                                onPago = { viewModel.alternarPago(fila) },
                                onCambiar = { cambiando = fila },
                                onQuitar = { quitando = fila }
                            )
                        }
                        TextButton(onClick = { vaciando = true }) { Text("Vaciar lista (nueva noche)") }
                    }
                    Box(Modifier.height(8.dp))
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

    cambiando?.let { fila ->
        DialogoCambiar(
            fila = fila,
            onCambiar = { numero -> viewModel.cambiar(fila, numero).also { if (it == null) cambiando = null } },
            onCancelar = { cambiando = null }
        )
    }

    quitando?.let { fila ->
        AlertDialog(
            onDismissRequest = { quitando = null },
            title = { Text("¿Quitar el tablero ${fila.numero}?") },
            text = { Text("Sale de la lista: ya no juega esta ronda ni las siguientes.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.quitar(fila)
                    quitando = null
                }) { Text("Quitar") }
            },
            dismissButton = { TextButton(onClick = { quitando = null }) { Text("Cancelar") } }
        )
    }

    if (vaciando) {
        AlertDialog(
            onDismissRequest = { vaciando = false },
            title = { Text("¿Vaciar la lista?") },
            text = { Text("Se quitan todos los tableros. Úsalo al empezar una noche nueva.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.vaciarLista()
                    vaciando = false
                }) { Text("Vaciar") }
            },
            dismissButton = { TextButton(onClick = { vaciando = false }) { Text("Cancelar") } }
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
                "${estado.pagados} de ${estado.filas.size} tableros pagaron",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Tablero de 4: ${Dinero.formato(estado.precioCuatro)} · de 8: ${Dinero.formato(estado.precioCuatro * 2)}",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun AgregarTablero(viewModel: CobroViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = viewModel.numeroTexto,
            onValueChange = viewModel::cambiarNumero,
            label = { Text("Número del tablero") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.agregar() }),
            modifier = Modifier.weight(1f)
        )
        Button(onClick = viewModel::agregar, enabled = viewModel.numeroTexto.isNotEmpty()) { Text("Agregar") }
    }
    viewModel.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}

/** Un tablero de la lista: botón grande Pagó/Debe, y opciones de cambiar o quitar. */
@Composable
private fun FilaTablero(
    fila: FilaCobro,
    valor: Int,
    onPago: () -> Unit,
    onCambiar: () -> Unit,
    onQuitar: () -> Unit
) {
    val colores = MaterialTheme.colorScheme
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("Tablero ${fila.numero}", style = MaterialTheme.typography.titleLarge)
                Text("${fila.cantidadTablas} tablas · ${Dinero.formato(valor)}", style = MaterialTheme.typography.bodyMedium)
                Row {
                    TextButton(onClick = onCambiar) { Text("Cambiar") }
                    TextButton(onClick = onQuitar) { Text("Quitar") }
                }
            }
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(64.dp)
                    .background(if (fila.pagado) colores.primary else colores.errorContainer, RoundedCornerShape(12.dp))
                    .clickable(onClick = onPago),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (fila.pagado) "✓ Pagó" else "Debe",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (fila.pagado) colores.onPrimary else colores.onErrorContainer
                )
            }
        }
    }
}

/** Cambiar el tablero de una persona por otro que esté libre. */
@Composable
private fun DialogoCambiar(
    fila: FilaCobro,
    onCambiar: (Int) -> String?,
    onCancelar: () -> Unit
) {
    var numero by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Cambiar el tablero ${fila.numero}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("¿Por cuál tablero lo cambia? Se conserva si ya pagó.")
                OutlinedTextField(
                    value = numero,
                    onValueChange = {
                        numero = it.filter(Char::isDigit).take(4)
                        error = null
                    },
                    label = { Text("Número del tablero nuevo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { numero.toIntOrNull()?.let { error = onCambiar(it) } },
                enabled = numero.isNotEmpty()
            ) { Text("Cambiar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
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
