package com.orlando.bingoapp.ui.tableros

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
import com.orlando.bingoapp.data.entity.TableroConTablas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaTablerosScreen(
    onNuevoTablero: () -> Unit,
    onVerTablero: (Long) -> Unit,
    viewModel: TablerosViewModel = viewModel()
) {
    val tableros by viewModel.tableros.collectAsStateWithLifecycle()
    var aEliminar by remember { mutableStateOf<TableroConTablas?>(null) }
    var menuCopia by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tableros (${tableros.size})") },
                actions = { TextButton(onClick = { menuCopia = true }) { Text("Copia") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNuevoTablero) { Text("+  Nuevo tablero") }
        }
    ) { padding ->
        if (tableros.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Todavía no hay tableros.\nToca \"Nuevo tablero\" para registrar el primero.",
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
                items(tableros, key = { it.tablero.id }) { item ->
                    TarjetaTablero(
                        item = item,
                        onVer = { onVerTablero(item.tablero.id) },
                        onEliminar = { aEliminar = item }
                    )
                }
            }
        }
    }

    // Confirmación antes de borrar (borra también sus tablas)
    aEliminar?.let { item ->
        AlertDialog(
            onDismissRequest = { aEliminar = null },
            title = { Text("¿Eliminar tablero ${item.tablero.numero}?") },
            text = { Text("Se borrarán también sus ${item.tablas.size} tablas.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminar(item.tablero)
                    aEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { aEliminar = null }) { Text("Cancelar") } }
        )
    }

    if (menuCopia) {
        DialogoCopia(viewModel = viewModel, onCerrar = { menuCopia = false })
    }
}

@Composable
private fun TarjetaTablero(item: TableroConTablas, onVer: () -> Unit, onEliminar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onVer)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Tablero ${item.tablero.numero}", style = MaterialTheme.typography.titleLarge)
                Text("${item.tablas.size} tablas · toca para ver y editar", style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onVer) { Text("Ver") }
            TextButton(onClick = onEliminar) { Text("Eliminar") }
        }
    }
}
