package com.orlando.bingoapp.ui.jugadas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorJugadaScreen(
    viewModel: EditorJugadaViewModel,
    onSalir: () -> Unit
) {
    LaunchedEffect(viewModel.guardado) {
        if (viewModel.guardado) onSalir()
    }
    BackHandler(onBack = onSalir)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.esNueva) "Nueva jugada" else "Editar jugada") },
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
            OutlinedTextField(
                value = viewModel.nombre,
                onValueChange = viewModel::cambiarNombre,
                label = { Text("Nombre (ej: Cuadrito de 4)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Card(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Vale en cualquier posición", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Dibuja la figura una vez y vale en cualquier parte de la tabla. " +
                                "Ej: un cuadrito 2x2 vale en las 16 posiciones.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = viewModel.cualquierPosicion,
                        onCheckedChange = viewModel::cambiarCualquierPosicion
                    )
                }
            }

            viewModel.figuras.forEachIndexed { i, figura ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (viewModel.figuras.size > 1) "Figura ${i + 1}" else "Figura",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (viewModel.figuras.size > 1) {
                        TextButton(onClick = { viewModel.quitarFigura(i) }) { Text("Quitar") }
                    }
                }
                FiguraEditable(
                    figura = figura,
                    onToque = { casilla -> viewModel.alternar(i, casilla) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedButton(onClick = viewModel::agregarFigura, modifier = Modifier.fillMaxWidth()) {
                Text("+ Otra figura (ej: la otra diagonal)")
            }

            Text(
                "Formas de ganar con esta jugada: ${viewModel.posicionesTotales}",
                style = MaterialTheme.typography.titleMedium
            )
            viewModel.mensajeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(onClick = viewModel::guardar, modifier = Modifier.fillMaxWidth()) {
                Text("Guardar jugada", fontSize = 18.sp)
            }
        }
    }
}
