package com.orlando.bingoapp.ui.juego

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orlando.bingoapp.domain.ReglasBingo

/**
 * Una tabla pequeña de solo lectura: muestra los números cantados en color
 * y, si ganó, resalta las casillas de la figura ganadora.
 */
@Composable
fun TablaMini(
    numeros: List<Int>,
    balotas: Set<Int>,
    figuraGanadora: String?,
    modifier: Modifier = Modifier
) {
    val colores = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (fila in 0 until 5) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (columna in 0 until 5) {
                    val indice = fila * 5 + columna
                    val esCentro = indice == ReglasBingo.INDICE_CENTRO
                    val numero = numeros[indice]
                    val marcada = esCentro || numero in balotas
                    val deLaFigura = figuraGanadora?.getOrNull(indice) == '1'

                    val (fondo, texto) = when {
                        deLaFigura -> colores.tertiary to colores.onTertiary
                        marcada -> colores.primary to colores.onPrimary
                        else -> colores.surfaceVariant to colores.onSurfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(fondo, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (esCentro) "★" else "$numero",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = texto
                        )
                    }
                }
            }
        }
    }
}
