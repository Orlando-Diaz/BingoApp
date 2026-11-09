package com.orlando.bingoapp.ui.seguridad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orlando.bingoapp.data.Ajustes
import com.orlando.bingoapp.domain.Patron

private enum class Paso { CREAR, CONFIRMAR, DESBLOQUEAR }

/**
 * Pide el patrón antes de mostrar la pestaña Tableros, para que nadie borre
 * o cambie tableros por accidente. La primera vez pide crearlo.
 */
@Composable
fun BloqueoTablerosScreen(onDesbloqueado: () -> Unit) {
    val ajustes = Ajustes(LocalContext.current)
    var paso by remember { mutableStateOf(if (ajustes.patronTableros == null) Paso.CREAR else Paso.DESBLOQUEAR) }
    var primerIntento by remember { mutableStateOf<List<Int>>(emptyList()) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var olvido by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            when (paso) {
                Paso.CREAR -> "Crea un patrón para proteger los tableros"
                Paso.CONFIRMAR -> "Dibuja el patrón otra vez"
                Paso.DESBLOQUEAR -> "Dibuja el patrón para entrar a Tableros"
            },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        if (paso == Paso.CREAR) {
            Text(
                "Así nadie borra o cambia un tablero por equivocación. Une al menos ${Patron.MINIMO_PUNTOS} puntos.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        PatronDibujo(
            onTerminar = { puntos ->
                when (paso) {
                    Paso.CREAR -> if (Patron.esValido(puntos)) {
                        primerIntento = puntos
                        paso = Paso.CONFIRMAR
                        mensaje = null
                    } else {
                        mensaje = "Une al menos ${Patron.MINIMO_PUNTOS} puntos"
                    }
                    Paso.CONFIRMAR -> if (puntos == primerIntento) {
                        ajustes.patronTableros = Patron.huella(puntos)
                        onDesbloqueado()
                    } else {
                        paso = Paso.CREAR
                        mensaje = "No coincidió. Empieza de nuevo."
                    }
                    Paso.DESBLOQUEAR -> {
                        val guardado = ajustes.patronTableros
                        if (guardado != null && Patron.coincide(puntos, guardado)) onDesbloqueado()
                        else mensaje = "Patrón incorrecto"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        mensaje?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (paso == Paso.DESBLOQUEAR) {
            TextButton(onClick = { olvido = true }) { Text("Olvidé el patrón") }
        }
    }

    if (olvido) {
        var texto by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { olvido = false },
            title = { Text("¿Crear un patrón nuevo?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Para confirmar, escribe BORRAR.")
                    OutlinedTextField(value = texto, onValueChange = { texto = it }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        ajustes.patronTableros = null
                        paso = Paso.CREAR
                        mensaje = null
                        olvido = false
                    },
                    enabled = texto.trim().equals("BORRAR", ignoreCase = true)
                ) { Text("Crear nuevo") }
            },
            dismissButton = { TextButton(onClick = { olvido = false }) { Text("Cancelar") } }
        )
    }
}
