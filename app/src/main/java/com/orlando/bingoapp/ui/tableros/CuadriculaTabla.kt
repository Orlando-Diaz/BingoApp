package com.orlando.bingoapp.ui.tableros

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orlando.bingoapp.domain.ReglasBingo

/**
 * Cuadrícula 5x5 para escribir los números de una tabla.
 * Al completar una casilla salta sola a la siguiente (columna por columna).
 *
 * @param claveTabla cambia cuando se pasa a otra tabla, para volver a enfocar la primera casilla.
 */
@Composable
fun CuadriculaTabla(
    casillas: List<String>,
    errores: Map<Int, String>,
    claveTabla: Int,
    onCambio: (indice: Int, texto: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focos = remember(claveTabla) { List(25) { FocusRequester() } }
    val focusManager = LocalFocusManager.current
    val orden = ReglasBingo.ORDEN_LLENADO

    // Al abrir una tabla, el cursor queda en la primera casilla de la B
    LaunchedEffect(claveTabla) { focos[orden.first()].requestFocus() }

    fun irASiguiente(indice: Int) {
        val pos = orden.indexOf(indice)
        if (pos < orden.lastIndex) focos[orden[pos + 1]].requestFocus()
        else focusManager.clearFocus() // última casilla: se cierra el teclado
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Encabezado B I N G O
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ReglasBingo.LETRAS.forEach { letra ->
                Text(
                    text = letra,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        for (fila in 0 until 5) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (columna in 0 until 5) {
                    val indice = fila * 5 + columna
                    val celda = Modifier.weight(1f).aspectRatio(1f)
                    if (indice == ReglasBingo.INDICE_CENTRO) {
                        CasillaLibre(celda)
                    } else {
                        CasillaNumero(
                            texto = casillas[indice],
                            conError = indice in errores,
                            esUltima = indice == orden.last(),
                            foco = focos[indice],
                            onCambio = { nuevo ->
                                onCambio(indice, nuevo)
                                if (estaCompleta(nuevo, columna)) irASiguiente(indice)
                            },
                            onSiguiente = { irASiguiente(indice) },
                            modifier = celda
                        )
                    }
                }
            }
        }
    }
}

/**
 * Una casilla está completa si ya tiene 2 dígitos, o si con 1 dígito
 * ya no cabe otro (ej: en la B, un "3" no puede ser "3x" porque la B llega hasta 15).
 */
private fun estaCompleta(texto: String, columna: Int): Boolean {
    val limpio = texto.filter { it.isDigit() }
    if (limpio.length >= 2) return true
    val digito = limpio.toIntOrNull() ?: return false
    return digito * 10 > ReglasBingo.rangoColumna(columna).last
}

@Composable
private fun CasillaNumero(
    texto: String,
    conError: Boolean,
    esUltima: Boolean,
    foco: FocusRequester,
    onCambio: (String) -> Unit,
    onSiguiente: () -> Unit,
    modifier: Modifier
) {
    val colores = MaterialTheme.colorScheme
    val colorBorde = if (conError) colores.error else colores.outline
    val fondo = if (conError) colores.errorContainer else colores.surface

    Box(
        modifier = modifier
            .border(2.dp, colorBorde, RoundedCornerShape(8.dp))
            .background(fondo, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = texto,
            onValueChange = onCambio,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = if (conError) colores.onErrorContainer else colores.onSurface
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = if (esUltima) ImeAction.Done else ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { onSiguiente() }, onDone = { onSiguiente() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .focusRequester(foco)
        )
    }
}

@Composable
private fun CasillaLibre(modifier: Modifier) {
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.primaryContainer,
            RoundedCornerShape(8.dp)
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "LIBRE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
