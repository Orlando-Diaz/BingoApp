package com.orlando.bingoapp.ui.jugadas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orlando.bingoapp.data.entity.Modo
import com.orlando.bingoapp.domain.Figuras

/** Las formas de ganar en los sencillos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JugadasScreen(
    onNueva: () -> Unit,
    onEditar: (Modo) -> Unit,
    viewModel: JugadasViewModel = viewModel()
) {
    val jugadas by viewModel.jugadas.collectAsStateWithLifecycle()
    var aEliminar by remember { mutableStateOf<Modo?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Jugadas de los sencillos") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNueva) { Text("+  Nueva jugada") }
        }
    ) { padding ->
        if (jugadas.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No hay jugadas todavía.\nAgrega las formas de ganar los sencillos " +
                        "(4 esquinas, cuadrito, líneas...).",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(jugadas, key = { it.id }) { jugada ->
                    TarjetaJugada(
                        jugada = jugada,
                        onEditar = { onEditar(jugada) },
                        onEliminar = { aEliminar = jugada }
                    )
                }
            }
        }
    }

    aEliminar?.let { jugada ->
        AlertDialog(
            onDismissRequest = { aEliminar = null },
            title = { Text("¿Quitar \"${jugada.nombre}\"?") },
            text = { Text("Ya no valdrá para ganar en los sencillos.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminar(jugada)
                    aEliminar = null
                }) { Text("Quitar") }
            },
            dismissButton = { TextButton(onClick = { aEliminar = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TarjetaJugada(jugada: Modo, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val posiciones = Figuras.expandir(jugada.figuras, jugada.cualquierPosicion).size
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onEditar)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(jugada.nombre, style = MaterialTheme.typography.titleLarge)
                    Text(
                        (if (jugada.cualquierPosicion) "En cualquier posición · " else "") +
                            "$posiciones forma(s) de ganar",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                TextButton(onClick = onEliminar) { Text("Quitar") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                jugada.figuras.take(5).forEach { FiguraMini(it) }
            }
        }
    }
}
