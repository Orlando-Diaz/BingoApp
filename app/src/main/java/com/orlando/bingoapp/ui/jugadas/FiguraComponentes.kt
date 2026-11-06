package com.orlando.bingoapp.ui.jugadas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orlando.bingoapp.domain.ReglasBingo

/**
 * Cuadrícula 5x5 para dibujar una figura: cada toque prende o apaga una casilla.
 * El centro es la casilla LIBRE: no se puede tocar porque nunca sirve para ganar.
 */
@Composable
fun FiguraEditable(
    figura: String,
    onToque: (indice: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colores = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ReglasBingo.LETRAS.forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = colores.primary
                )
            }
        }
        for (fila in 0 until 5) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (columna in 0 until 5) {
                    val indice = fila * 5 + columna
                    val esCentro = indice == ReglasBingo.INDICE_CENTRO
                    val activa = !esCentro && figura[indice] == '1'
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(
                                when {
                                    esCentro -> colores.outlineVariant
                                    activa -> colores.primary
                                    else -> colores.surfaceVariant
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, colores.outline, RoundedCornerShape(8.dp))
                            .clickable(enabled = !esCentro) { onToque(indice) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (esCentro) {
                            Text(
                                "LIBRE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colores.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Vista pequeña de una figura, de solo lectura (para listas). */
@Composable
fun FiguraMini(figura: String, tamano: Dp = 56.dp) {
    val colores = MaterialTheme.colorScheme
    val celda = (tamano - 8.dp) / 5
    Column(
        modifier = Modifier.size(tamano),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (fila in 0 until 5) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (columna in 0 until 5) {
                    Box(
                        Modifier
                            .size(celda)
                            .background(
                                if (fila * 5 + columna != ReglasBingo.INDICE_CENTRO && figura[fila * 5 + columna] == '1') colores.primary
                                else colores.surfaceVariant,
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}

/** Prende o apaga una casilla de la figura. */
fun alternarCasilla(figura: String, indice: Int): String {
    if (indice == ReglasBingo.INDICE_CENTRO) return figura
    val casillas = figura.toCharArray()
    casillas[indice] = if (casillas[indice] == '1') '0' else '1'
    return String(casillas)
}
