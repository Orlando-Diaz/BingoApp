package com.orlando.bingoapp.ui.tableros

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orlando.bingoapp.camara.BotonFotoTabla
import com.orlando.bingoapp.camara.FotoTabla

/** Corregir o reemplazar los números de una tabla. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTablaScreen(
    viewModel: EditorTablaViewModel,
    onSalir: () -> Unit
) {
    LaunchedEffect(viewModel.guardado) {
        if (viewModel.guardado) onSalir()
    }
    BackHandler(onBack = onSalir)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tablero ${viewModel.numeroTablero} · Tabla ${viewModel.posicion}") },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Corrige los números, o toma una foto si pegaron una tabla nueva.",
                style = MaterialTheme.typography.bodyMedium
            )
            BotonFotoTabla(
                leyendo = viewModel.leyendoFoto,
                tieneFoto = viewModel.foto != null,
                mensaje = viewModel.mensajeFoto,
                onPreparar = viewModel::prepararFoto,
                onResultado = viewModel::fotoTomada
            )
            viewModel.foto?.let { FotoTabla(it, Modifier.fillMaxWidth().heightIn(max = 320.dp)) }

            CuadriculaTabla(
                casillas = viewModel.casillas,
                errores = viewModel.erroresActuales(),
                claveTabla = viewModel.posicion,
                onCambio = viewModel::cambiarCasilla,
                modifier = Modifier.fillMaxWidth()
            )
            viewModel.erroresActuales().values.distinct().forEach {
                Text("• $it", color = MaterialTheme.colorScheme.error)
            }
            viewModel.mensajeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(onClick = viewModel::guardar, modifier = Modifier.fillMaxWidth()) {
                Text("Guardar cambios", fontSize = 18.sp)
            }
        }
    }
}
