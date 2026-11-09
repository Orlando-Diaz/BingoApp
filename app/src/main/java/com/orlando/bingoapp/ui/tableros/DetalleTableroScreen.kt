package com.orlando.bingoapp.ui.tableros

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orlando.bingoapp.data.entity.Tabla
import com.orlando.bingoapp.ui.juego.TablaMini
import kotlinx.coroutines.launch

/** Un tablero con todas sus tablas a la vista; cada tabla se puede editar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleTableroScreen(
    viewModel: DetalleTableroViewModel,
    onEditarTabla: (tabla: Tabla, numeroTablero: Int) -> Unit,
    onSalir: () -> Unit
) {
    val tablero by viewModel.tablero.collectAsStateWithLifecycle()
    var cambiandoNumero by remember { mutableStateOf(false) }
    var eliminando by remember { mutableStateOf(false) }

    BackHandler(onBack = onSalir)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tablero?.let { "Tablero ${it.tablero.numero}" } ?: "Tablero") },
                navigationIcon = { TextButton(onClick = onSalir) { Text("Volver") } },
                actions = { TextButton(onClick = { cambiandoNumero = true }) { Text("Cambiar número") } }
            )
        }
    ) { padding ->
        val actual = tablero ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "${actual.tablas.size} tablas · toca \"Editar\" si una tabla quedó mal o la cambiaron por otra.",
                style = MaterialTheme.typography.bodyMedium
            )
            actual.tablas.sortedBy { it.posicion }.forEach { tabla ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tabla ${tabla.posicion}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            Button(onClick = { onEditarTabla(tabla, actual.tablero.numero) }) { Text("Editar") }
                        }
                        TablaMini(
                            numeros = tabla.numeros,
                            balotas = emptySet(),
                            figuraGanadora = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            TextButton(onClick = { eliminando = true }) {
                Text("Eliminar este tablero", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (cambiandoNumero) {
        DialogoCambiarNumero(
            numeroActual = tablero?.tablero?.numero ?: 0,
            onCambiar = viewModel::cambiarNumero,
            onCerrar = { cambiandoNumero = false }
        )
    }

    if (eliminando) {
        val numero = tablero?.tablero?.numero
        var confirmacion by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { eliminando = false },
            title = { Text("¿Eliminar el tablero $numero?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Se borran sus tablas y no se puede deshacer. Si solo no va a jugar esta ronda, " +
                            "quítalo en Cobro, no aquí."
                    )
                    Text("Para confirmar, escribe el número del tablero:")
                    OutlinedTextField(
                        value = confirmacion,
                        onValueChange = { confirmacion = it.filter(Char::isDigit).take(4) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        eliminando = false
                        viewModel.eliminar(alTerminar = onSalir)
                    },
                    enabled = confirmacion == numero?.toString()
                ) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { eliminando = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun DialogoCambiarNumero(
    numeroActual: Int,
    onCambiar: suspend (Int) -> String?,
    onCerrar: () -> Unit
) {
    var texto by remember { mutableStateOf(numeroActual.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val alcance = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Cambiar número del tablero") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = {
                        texto = it.filter(Char::isDigit).take(4)
                        error = null
                    },
                    label = { Text("Número nuevo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val nuevo = texto.toIntOrNull() ?: return@TextButton
                    alcance.launch {
                        error = onCambiar(nuevo)
                        if (error == null) onCerrar()
                    }
                },
                enabled = texto.isNotEmpty()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
