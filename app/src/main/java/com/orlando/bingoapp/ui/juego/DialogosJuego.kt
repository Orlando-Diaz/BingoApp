package com.orlando.bingoapp.ui.juego

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.orlando.bingoapp.domain.Figuras
import com.orlando.bingoapp.ui.jugadas.FiguraEditable
import com.orlando.bingoapp.ui.jugadas.alternarCasilla

/** Elegir si la nueva partida es sencillo o llena. */
@Composable
fun DialogoNuevaPartida(
    onSencillo: () -> Unit,
    onLlena: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("¿Qué se va a jugar?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onSencillo, modifier = Modifier.fillMaxWidth()) { Text("Sencillo") }
                Button(onClick = onLlena, modifier = Modifier.fillMaxWidth()) { Text("Llena (con premios)") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

/** Un premio intermedio mientras se está configurando. */
private class PremioEnEdicion(numero: Int) {
    var nombre by mutableStateOf("Premio $numero")
    var figura by mutableStateOf(Figuras.VACIA)
    var porcentajeTexto by mutableStateOf("")
}

/**
 * Configurar la llena: 1 o 2 premios intermedios. Para cada uno se DIBUJA
 * la figura en ese momento y se pone el porcentaje. El cartón lleno se lleva lo que sobre.
 */
@Composable
fun DialogoConfigurarLlena(
    onIniciar: (List<PremioIntermedio>) -> Unit,
    onCancelar: () -> Unit
) {
    val premios = remember { mutableStateListOf(PremioEnEdicion(1)) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Premios de la llena") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                premios.forEach { premio ->
                    OutlinedTextField(
                        value = premio.nombre,
                        onValueChange = { premio.nombre = it.take(30) },
                        label = { Text("Nombre (ej: La X)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Toca las casillas para dibujar la figura", style = MaterialTheme.typography.bodySmall)
                    FiguraEditable(
                        figura = premio.figura,
                        onToque = { premio.figura = alternarCasilla(premio.figura, it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = premio.porcentajeTexto,
                        onValueChange = { premio.porcentajeTexto = it.filter(Char::isDigit).take(2) },
                        label = { Text("Porcentaje del premio (%)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(220.dp)
                    )
                    HorizontalDivider()
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (premios.size < 2) {
                        OutlinedButton(onClick = { premios.add(PremioEnEdicion(premios.size + 1)) }) { Text("+ Otro premio") }
                    }
                    if (premios.size > 1) {
                        OutlinedButton(onClick = { premios.removeAt(premios.lastIndex) }) { Text("Quitar último") }
                    }
                }

                val suma = premios.sumOf { it.porcentajeTexto.toIntOrNull() ?: 0 }
                Text("Cartón lleno: ${100 - suma}% del premio", style = MaterialTheme.typography.titleMedium)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val porcentajes = premios.map { it.porcentajeTexto.toIntOrNull() ?: 0 }
                error = when {
                    premios.any { it.nombre.isBlank() } -> "Ponle nombre a cada premio"
                    premios.any { Figuras.cantidadCasillas(it.figura) == 0 } -> "Dibuja la figura de cada premio"
                    porcentajes.any { it <= 0 } -> "Escribe el porcentaje de cada premio"
                    porcentajes.sum() >= 100 -> "Los premios suman 100% o más: no queda nada para el lleno"
                    else -> null
                }
                if (error == null) {
                    onIniciar(premios.map {
                        PremioIntermedio(it.nombre.trim(), it.figura, it.porcentajeTexto.toInt())
                    })
                }
            }) { Text("Iniciar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

/** Pide el número del tablero y muestra sus tablas con lo cantado. */
@Composable
fun DialogoVerificarTablero(
    verificacion: Verificacion?,
    balotas: Set<Int>,
    onBuscar: (Int) -> Unit,
    onCerrar: () -> Unit
) {
    var numeroTexto by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Verificar tablero") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = numeroTexto,
                        onValueChange = { numeroTexto = it.filter(Char::isDigit).take(4) },
                        label = { Text("Número") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { numeroTexto.toIntOrNull()?.let(onBuscar) },
                        enabled = numeroTexto.isNotEmpty()
                    ) { Text("Buscar") }
                }

                when {
                    verificacion == null -> {}
                    !verificacion.encontrado ->
                        Text("No hay ningún tablero con el número ${verificacion.numero}", color = MaterialTheme.colorScheme.error)
                    else -> {
                        val ganadoras = verificacion.tablas.filter { it.ganadora != null }
                        Text(
                            if (ganadoras.isEmpty()) "Tablero ${verificacion.numero}: ninguna tabla ha ganado todavía"
                            else "¡Tablero ${verificacion.numero} tiene ${ganadoras.size} tabla(s) ganadora(s)!",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (ganadoras.isEmpty()) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.tertiary
                        )
                        verificacion.tablas.forEach { tabla ->
                            val detalle = when {
                                !tabla.pagada -> " — no pagada, no juega"
                                tabla.ganadora != null -> " — ganó con ${tabla.ganadora.nombreModo}"
                                else -> ""
                            }
                            Text(
                                "Tabla ${tabla.posicion}$detalle",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            TablaMini(
                                numeros = tabla.numeros,
                                balotas = balotas,
                                figuraGanadora = tabla.ganadora?.figura,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}
