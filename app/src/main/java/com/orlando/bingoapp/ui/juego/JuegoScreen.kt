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
import com.orlando.bingoapp.domain.ReglasBingo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuegoScreen(viewModel: JuegoViewModel = viewModel()) {
    val partida by viewModel.partida.collectAsStateWithLifecycle()
    val balotas = partida?.balotas.orEmpty()

    var balotaAQuitar by remember { mutableStateOf<Int?>(null) }
    var confirmarNueva by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Juego") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UltimaBalota(balotas)
            UltimasCantadas(balotas)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = viewModel::deshacerUltima,
                    enabled = balotas.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text("Deshacer última") }
                Button(
                    onClick = { confirmarNueva = true },
                    modifier = Modifier.weight(1f)
                ) { Text("Nueva partida") }
            }

            TableroBalotas(
                balotas = balotas,
                onToque = { balota ->
                    // Si no está marcada, se marca. Si ya está, se pregunta antes de quitarla.
                    if (balota in balotas) balotaAQuitar = balota else viewModel.marcar(balota)
                }
            )
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

    if (confirmarNueva) {
        AlertDialog(
            onDismissRequest = { confirmarNueva = false },
            title = { Text("¿Empezar una partida nueva?") },
            text = { Text("Se limpian las ${balotas.size} balotas cantadas.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.nuevaPartida()
                    confirmarNueva = false
                }) { Text("Empezar") }
            },
            dismissButton = { TextButton(onClick = { confirmarNueva = false }) { Text("Cancelar") } }
        )
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
private fun TableroBalotas(balotas: List<Int>, onToque: (Int) -> Unit) {
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
                            .clickable { onToque(balota) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$balota", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = texto)
                    }
                }
            }
        }
    }
}
