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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroTableroScreen(
    viewModel: RegistroTableroViewModel,
    onSalir: () -> Unit
) {
    // Cuando se guarda, volvemos a la lista
    LaunchedEffect(viewModel.guardado) {
        if (viewModel.guardado) onSalir()
    }

    // Botón "atrás" del teléfono
    BackHandler {
        if (viewModel.paso == PasoRegistro.DATOS) onSalir() else viewModel.anterior()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (viewModel.paso == PasoRegistro.DATOS) "Nuevo tablero"
                        else "Tablero ${viewModel.numeroTexto}"
                    )
                },
                navigationIcon = { TextButton(onClick = onSalir) { Text("Cancelar") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (viewModel.paso) {
                PasoRegistro.DATOS -> PasoDatos(viewModel)
                PasoRegistro.TABLAS -> PasoTablas(viewModel)
            }
        }
    }
}

@Composable
private fun PasoDatos(viewModel: RegistroTableroViewModel) {
    OutlinedTextField(
        value = viewModel.numeroTexto,
        onValueChange = viewModel::cambiarNumero,
        label = { Text("Número del tablero") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )

    Text("¿Cuántas tablas tiene?", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(4, 8).forEach { cantidad ->
            FilterChip(
                selected = viewModel.cantidadTablas == cantidad,
                onClick = { viewModel.cambiarCantidad(cantidad) },
                label = { Text("$cantidad tablas", fontSize = 18.sp) }
            )
        }
    }

    MensajeError(viewModel.mensajeError)

    Button(onClick = viewModel::confirmarDatos, modifier = Modifier.fillMaxWidth()) {
        Text("Continuar", fontSize = 18.sp)
    }
}

@Composable
private fun PasoTablas(viewModel: RegistroTableroViewModel) {
    val actual = viewModel.tablaActual + 1
    val total = viewModel.cantidadTablas
    val esUltima = actual == total

    Text("Tabla $actual de $total", style = MaterialTheme.typography.titleLarge)
    LinearProgressIndicator(
        progress = { actual.toFloat() / total },
        modifier = Modifier.fillMaxWidth()
    )

    CuadriculaTabla(
        casillas = viewModel.casillasActuales,
        errores = viewModel.erroresActuales(),
        claveTabla = viewModel.tablaActual,
        onCambio = viewModel::cambiarCasilla,
        modifier = Modifier.fillMaxWidth()
    )

    // Detalle de errores (ej: "La B va de 1 a 15")
    viewModel.erroresActuales().values.distinct().forEach {
        Text("• $it", color = MaterialTheme.colorScheme.error)
    }
    MensajeError(viewModel.mensajeError)

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = viewModel::anterior, modifier = Modifier.weight(1f)) {
            Text("Anterior")
        }
        Button(onClick = viewModel::siguiente, modifier = Modifier.weight(1f)) {
            Text(if (esUltima) "Guardar tablero" else "Siguiente")
        }
    }
}

@Composable
private fun MensajeError(mensaje: String?) {
    if (mensaje != null) {
        Text(mensaje, color = MaterialTheme.colorScheme.error)
    }
}
